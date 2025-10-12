package com.wallet_service.be.security;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@WebFilter("/*")
public class AccessLogFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        long start = System.currentTimeMillis();
        chain.doFilter(request, response);
        long duration = System.currentTimeMillis() - start;

        String timestamp = "[" + LocalDateTime.now().format(formatter) + "]";
        String clientIp = req.getRemoteAddr();
        String method = req.getMethod();
        String uri = req.getRequestURI();
        int status = res.getStatus();

        log.info("{} {} {} {} - {} ({} ms)", timestamp, clientIp, method, uri, status, duration);
    }
}
