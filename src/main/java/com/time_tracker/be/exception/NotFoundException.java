package com.time_tracker.be.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotFoundException extends RuntimeException {
    private final int statusCode;
    private final LocalDateTime timestamp;

    public NotFoundException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.timestamp = LocalDateTime.now();
    }

    public NotFoundException(String message) {
        super(message);
        this.statusCode = 404;
        this.timestamp = LocalDateTime.now();
    }
}
