package com.wallet_service.be.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet_service.be.annotation.RequireAuth;
import com.wallet_service.be.annotation.RequirePermission;
import com.wallet_service.be.annotation.RequireRole;
import com.wallet_service.be.exception.ForbiddenException;
import com.wallet_service.be.exception.NotAuthorizedException;
import com.wallet_service.be.lib.JwtService;
import com.wallet_service.be.utils.HmacUtils;
import com.wallet_service.be.utils.commons.PermissionActionDto;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class JwtFilter {
    private final JwtService jwtService;
    private final HttpServletRequest request;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final HmacUtils hmacUtils;
    private final String accountServiceUrl;

    public JwtFilter(JwtService jwtService,
                     HttpServletRequest request,
                     RedisTemplate<String, Object> redisTemplate,
                     ObjectMapper objectMapper,
                     HmacUtils hmacUtils,
                     @Value("${app.account-service.url:http://localhost:8080}") String accountServiceUrl) {
        this.jwtService = jwtService;
        this.request = request;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.hmacUtils = hmacUtils;
        this.accountServiceUrl = accountServiceUrl;
    }

    @Around("@annotation(requireAuth)")
    public Object checkAuth(ProceedingJoinPoint pjp, RequireAuth requireAuth) throws Throwable {
        String header = request.getHeader("Authorization");
        String token = jwtService.resolveToken(header);

        if (token == null || jwtService.getClaims(token).getExpiration().before(new Date())) {
            throw new NotAuthorizedException("Token is not valid");
        }

        return pjp.proceed();
    }

    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint pjp, RequireRole requireRole) throws Throwable {
        String header = request.getHeader("Authorization");
        String token = jwtService.resolveToken(header);

        if (token == null || jwtService.getClaims(token).getExpiration().before(new Date())) {
            throw new NotAuthorizedException("Token is not valid");
        }

        String role = (String) jwtService.getClaims(token).get("role");
        boolean hasRole = false;
        for (String r : requireRole.value()) {
            if (r.equals(role)) {
                hasRole = true;
                break;
            }
        }
        if (!hasRole) {
            throw new ForbiddenException("You don't have permission to access this resource");
        }

        return pjp.proceed();
    }

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint pjp, RequirePermission requirePermission) throws Throwable {
        String header = request.getHeader("Authorization");
        String token = jwtService.resolveToken(header);

        if (token == null) {
            throw new NotAuthorizedException("Token is missing");
        }

        Claims claims = jwtService.getClaims(token);
        if (claims.getExpiration().before(new Date())) {
            throw new NotAuthorizedException("Token is expired");
        }

        String role = (String) claims.get("role");
        Integer roleId = claims.get("roleId", Integer.class);

        // Super Admin or admin role always has full access
        if ("admin".equalsIgnoreCase(role) || (roleId != null && roleId == 1)) {
            return pjp.proceed();
        }

        if (roleId == null) {
            throw new ForbiddenException("Role information missing in token");
        }

        String key = "auth:role_permissions:" + roleId;
        Map<String, PermissionActionDto> permissions = null;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                String jsonString = cached instanceof String ? (String) cached : objectMapper.writeValueAsString(cached);
                permissions = objectMapper.readValue(jsonString, new TypeReference<Map<String, PermissionActionDto>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis error fetching role_permissions for key: {}, falling back to account-service", key, e);
        }

        // Cache miss or Redis error: Fallback to Account Service internal API
        if (permissions == null) {
            permissions = fetchPermissionsFromAccountService(roleId);
        }

        if (permissions == null) {
            throw new ForbiddenException("You don't have permission to access " + requirePermission.feature());
        }

        String featureKey = requirePermission.feature().toLowerCase();
        PermissionActionDto actionPerm = permissions.get(featureKey);

        if (actionPerm == null) {
            throw new ForbiddenException("You don't have permission to access " + requirePermission.feature());
        }

        boolean allowed = switch (requirePermission.action()) {
            case VIEW -> actionPerm.isView();
            case CREATE -> actionPerm.isCreate();
            case EDIT -> actionPerm.isEdit();
            case DELETE -> actionPerm.isDelete();
        };

        if (!allowed) {
            throw new ForbiddenException("You don't have permission to " + requirePermission.action().name().toLowerCase() + " " + requirePermission.feature());
        }

        return pjp.proceed();
    }

    private Map<String, PermissionActionDto> fetchPermissionsFromAccountService(int roleId) {
        try {
            String timestamp = Instant.now().toString();
            String signature = hmacUtils.generateHMAC(timestamp);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            String url = accountServiceUrl + "/api/internal/roles/" + roleId + "/permissions";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Signature", signature)
                    .header("X-Timestamp", timestamp)
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200 && resp.body() != null) {
                JsonNode jsonNode = objectMapper.readTree(resp.body());
                if (jsonNode.has("data") && jsonNode.get("data").isObject()) {
                    String dataJson = jsonNode.get("data").toString();
                    Map<String, PermissionActionDto> perms = objectMapper.readValue(
                            dataJson,
                            new TypeReference<Map<String, PermissionActionDto>>() {}
                    );

                    // Warm Redis cache (TTL 24h)
                    try {
                        String key = "auth:role_permissions:" + roleId;
                        redisTemplate.opsForValue().set(key, dataJson, Duration.ofHours(24));
                    } catch (Exception e) {
                        log.warn("Failed to warm Redis cache for role_permissions: {}", roleId, e);
                    }

                    return perms;
                }
            } else {
                log.warn("Failed internal request to account-service for role permissions. URL: {}, Status: {}", url, resp.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to fetch role permissions from account-service for roleId: {}", roleId, e);
        }
        return null;
    }
}
