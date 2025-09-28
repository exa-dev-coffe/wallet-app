package com.time_tracker.be.resolver;

import com.time_tracker.be.annotation.CurrentUser;
import com.time_tracker.be.lib.JwtService;
import com.time_tracker.be.utils.commons.CurrentUserDto;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {
    private final JwtService jwtService;

    public CurrentUserResolver(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
                parameter.getParameterType().equals(CurrentUserDto.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Claims claims = jwtService.getClaims(token); // decode token pakai jwt util kamu
            CurrentUserDto account = new CurrentUserDto();
            account.setUserId(claims.get("userId", Integer.class));
            account.setEmail(claims.get("email", String.class));
            account.setFullName(claims.get("fullName", String.class));
            return account;
        } else {
            return null; // atau lempar exception jika token tidak ada
        }

    }
}
