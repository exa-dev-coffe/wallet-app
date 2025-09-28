package com.time_tracker.be.exception;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Getter
@Component
public class TooManyRequestException extends RuntimeException {
    private final int statusCode;
    private final LocalDateTime timestamp;

    public TooManyRequestException(String message) {
        super(message);
        this.statusCode = 429;
        this.timestamp = LocalDateTime.now();
    }

    public TooManyRequestException() {
        this.statusCode = 429;
        this.timestamp = LocalDateTime.now();
    }

}
