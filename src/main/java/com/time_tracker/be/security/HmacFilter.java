package com.time_tracker.be.security;

import com.time_tracker.be.annotation.ValidateSignature;
import com.time_tracker.be.exception.NotAuthorizedException;
import com.time_tracker.be.utils.HmacUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Aspect
@Component
public class HmacFilter {

    private static final Logger log = LoggerFactory.getLogger(HmacFilter.class);
    private final HmacUtils hmacUtils;

    public HmacFilter(HmacUtils hmacUtils) {
        this.hmacUtils = hmacUtils;
    }

    @Around("@annotation(validateSignature)")
    public Object validateSignature(ProceedingJoinPoint pjp, ValidateSignature validateSignature) throws Throwable {
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        String signature = req.getHeader("X-Signature");
        String timestamp = req.getHeader("X-Timestamp");

        if (signature == null || timestamp == null) {
            throw new NotAuthorizedException("Missing signature or timestamp");
        }

        // ✅ Ambil body dari ContentCachingRequestWrapper
        String body = "";
        if (req instanceof ContentCachingRequestWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            body = new String(buf, wrapper.getCharacterEncoding());
        }

        String query = req.getQueryString() == null ? "" : req.getQueryString();
        String message = query + timestamp + body;

        if (!this.hmacUtils.verifySignature(message, signature)) {
            throw new NotAuthorizedException("Invalid signature");
        }

        return pjp.proceed();
    }
}
