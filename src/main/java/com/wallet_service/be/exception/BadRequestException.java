package com.wallet_service.be.exception;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BadRequestException extends RuntimeException {
    private final int statusCode;
    private final LocalDateTime timestamp;

    public BadRequestException(String message) {
        super(message);
        this.statusCode = 400;
        this.timestamp = java.time.LocalDateTime.now();
    }
}
