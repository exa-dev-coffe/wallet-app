package com.time_tracker.be.security;

import com.time_tracker.be.annotation.RequireAuth;
import com.time_tracker.be.exception.NotAuthorizedException;
import com.time_tracker.be.lib.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Date;

@Aspect
@Component
public class JwtFilter {
    private final JwtService jwtService;
    private final HttpServletRequest request;

    public JwtFilter(JwtService jwtService, HttpServletRequest request) {
        this.jwtService = jwtService;
        this.request = request;
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
}