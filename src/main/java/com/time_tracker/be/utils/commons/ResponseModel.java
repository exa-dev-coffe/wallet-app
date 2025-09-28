package com.time_tracker.be.utils.commons;

import lombok.Data;

import java.time.LocalDateTime;


@Data
public class ResponseModel<T> {
    private boolean success;
    private String message;
    private LocalDateTime timestamp;
    private T data;

    public ResponseModel() {
        this.success = true;
        this.message = "Request was successful";
        this.timestamp = LocalDateTime.now();
    }

    public ResponseModel(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
}
