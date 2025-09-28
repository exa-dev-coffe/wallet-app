package com.time_tracker.be.lib;

import com.time_tracker.be.account.AccountModel;
import com.time_tracker.be.exception.NotAuthorizedException;
import com.time_tracker.be.utils.enums.TokenType;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtService {

    @Value("${app.jwt.secret}")
    private String SECRET_KEY;

    private SecretKey getSigningKey() {
        return new SecretKeySpec(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8),
                SignatureAlgorithm.HS512.getJcaName()
        );
    }

    private Map<String, Object> createClaims(AccountModel user, TokenType type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("fullName", user.getFullName());
        claims.put("email", user.getEmail());
        claims.put("userId", user.getUserId());
        claims.put("type", type.name());
        claims.put("role", user.getRole().getRoleName());
        return claims;
    }

    private Date getExpirationDate(TokenType type) {
        long now = System.currentTimeMillis();
        return switch (type) {
            case ACCESS -> new Date(now + 1000L * 60 * 15); // 15 menit
            case REFRESH -> new Date(now + 1000L * 60 * 60 * 24 * 7); // 7 hari
            case RESET_PASSWORD -> new Date(now + 1000L * 60 * 5); // 5 menit
            default -> throw new IllegalArgumentException("Unknown token type: " + type);
        };
    }

    // Membuat token JWT
    public String createToken(AccountModel user, TokenType type) {
        return Jwts.builder()
                .setClaims(createClaims(user, type))
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(getExpirationDate(type))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // Ambil claims dari token
    public Claims getClaims(String token) {
        try {

            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new NotAuthorizedException("Token expired");
        } catch (MalformedJwtException e) {
            throw new NotAuthorizedException("Invalid token");
        } catch (UnsupportedJwtException e) {
            throw new NotAuthorizedException("Unsupported token");
        } catch (IllegalArgumentException e) {
            throw new NotAuthorizedException("Token is empty or null");
        } catch (Exception e) {
            throw new NotAuthorizedException("Token error");
        }
    }

    // Ambil email dari token
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    // Ambil token dari header Authorization
    public String resolveToken(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
