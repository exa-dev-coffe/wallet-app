package com.time_tracker.be.exception;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Getter
public class NotAuthorizedException extends RuntimeException {
    private final int statusCode;
    private final LocalDateTime timestamp;

    public NotAuthorizedException(String message) {
        super(message);
        this.statusCode = 401;
        this.timestamp = LocalDateTime.now();
    }

    public NotAuthorizedException() {
        this.statusCode = 401;
        this.timestamp = LocalDateTime.now();
    }
}
