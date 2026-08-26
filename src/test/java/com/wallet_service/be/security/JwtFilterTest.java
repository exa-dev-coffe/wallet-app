package com.wallet_service.be.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.wallet_service.be.annotation.ActionType;
import com.wallet_service.be.annotation.RequirePermission;
import com.wallet_service.be.exception.ForbiddenException;
import com.wallet_service.be.lib.JwtService;
import com.wallet_service.be.utils.HmacUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private RequirePermission requirePermission;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HmacUtils hmacUtils = new HmacUtils("secret");
    private JwtFilter jwtFilter;

    private static HttpServer mockAccountService;
    private static int mockPort;

    @BeforeAll
    static void startMockAccountService() throws Exception {
        mockAccountService = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        mockPort = mockAccountService.getAddress().getPort();

        // Handler for role 3 permissions (success)
        mockAccountService.createContext("/api/internal/roles/3/permissions", exchange -> {
            String sig = exchange.getRequestHeaders().getFirst("X-Signature");
            String ts = exchange.getRequestHeaders().getFirst("X-Timestamp");
            if (sig == null || ts == null) {
                exchange.sendResponseHeaders(401, 0);
                exchange.close();
                return;
            }
            String json = """
                    {
                        "status": 200,
                        "message": "Success",
                        "data": {
                            "posh": {
                                "view": true,
                                "create": true,
                                "edit": false,
                                "delete": false
                            }
                        },
                        "success": true
                    }
                    """;
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // Handler for role 4 permissions (error / missing)
        mockAccountService.createContext("/api/internal/roles/4/permissions", exchange -> {
            exchange.sendResponseHeaders(404, 0);
            exchange.close();
        });

        mockAccountService.start();
    }

    @AfterAll
    static void stopMockAccountService() {
        if (mockAccountService != null) {
            mockAccountService.stop(0);
        }
    }

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String accountServiceUrl = "http://localhost:" + mockPort;
        jwtFilter = new JwtFilter(jwtService, request, redisTemplate, objectMapper, hmacUtils, accountServiceUrl);
    }

    @Test
    void testCheckPermission_SuperAdmin_Allowed() throws Throwable {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.resolveToken("Bearer token")).thenReturn("token");

        Claims claims = new DefaultClaims();
        claims.put("role", "admin");
        claims.put("roleId", 1);
        claims.setExpiration(new Date(System.currentTimeMillis() + 60000));
        when(jwtService.getClaims("token")).thenReturn(claims);

        when(proceedingJoinPoint.proceed()).thenReturn("OK");

        Object result = jwtFilter.checkPermission(proceedingJoinPoint, requirePermission);
        assertEquals("OK", result);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void testCheckPermission_RedisHit_Allowed() throws Throwable {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.resolveToken("Bearer token")).thenReturn("token");

        Claims claims = new DefaultClaims();
        claims.put("role", "barista");
        claims.put("roleId", 3);
        claims.setExpiration(new Date(System.currentTimeMillis() + 60000));
        when(jwtService.getClaims("token")).thenReturn(claims);

        String cachedJson = """
                {
                    "posh": {
                        "view": true,
                        "create": false,
                        "edit": false,
                        "delete": false
                    }
                }
                """;
        when(valueOperations.get("auth:role_permissions:3")).thenReturn(cachedJson);

        when(requirePermission.feature()).thenReturn("posh");
        when(requirePermission.action()).thenReturn(ActionType.VIEW);
        when(proceedingJoinPoint.proceed()).thenReturn("OK");

        Object result = jwtFilter.checkPermission(proceedingJoinPoint, requirePermission);
        assertEquals("OK", result);
    }

    @Test
    void testCheckPermission_RedisMiss_FallbackSuccess_Allowed() throws Throwable {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.resolveToken("Bearer token")).thenReturn("token");

        Claims claims = new DefaultClaims();
        claims.put("role", "barista");
        claims.put("roleId", 3);
        claims.setExpiration(new Date(System.currentTimeMillis() + 60000));
        when(jwtService.getClaims("token")).thenReturn(claims);

        // Redis cache miss
        when(valueOperations.get("auth:role_permissions:3")).thenReturn(null);

        when(requirePermission.feature()).thenReturn("posh");
        when(requirePermission.action()).thenReturn(ActionType.VIEW);
        when(proceedingJoinPoint.proceed()).thenReturn("OK");

        Object result = jwtFilter.checkPermission(proceedingJoinPoint, requirePermission);
        assertEquals("OK", result);

        // Verify Redis cache warming occurred
        verify(valueOperations).set(eq("auth:role_permissions:3"), anyString(), eq(Duration.ofHours(24)));
    }

    @Test
    void testCheckPermission_RedisMiss_FallbackFailed_ThrowsForbidden() throws Throwable {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.resolveToken("Bearer token")).thenReturn("token");

        Claims claims = new DefaultClaims();
        claims.put("role", "barista");
        claims.put("roleId", 4); // role 4 returns 404
        claims.setExpiration(new Date(System.currentTimeMillis() + 60000));
        when(jwtService.getClaims("token")).thenReturn(claims);

        when(valueOperations.get("auth:role_permissions:4")).thenReturn(null);
        when(requirePermission.feature()).thenReturn("posh");

        assertThrows(ForbiddenException.class, () -> jwtFilter.checkPermission(proceedingJoinPoint, requirePermission));
    }
}


