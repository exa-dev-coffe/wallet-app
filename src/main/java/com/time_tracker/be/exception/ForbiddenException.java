package com.time_tracker.be.exception;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Getter
@Component
public class ForbiddenException extends RuntimeException {
    private final int statusCode;
    private final LocalDateTime timestamp;

    public ForbiddenException(String message) {
        super(message);
        this.statusCode = 403;
        this.timestamp = LocalDateTime.now();
    }

    public ForbiddenException() {
        this.statusCode = 403;
        this.timestamp = LocalDateTime.now();
    }
}
