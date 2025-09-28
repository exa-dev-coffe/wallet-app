package com.time_tracker.be.exception;

import com.time_tracker.be.resolver.ConstraintMessageResolver;
import com.time_tracker.be.utils.commons.ResponseModel;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ResponseModel<Map<String, Object>>> handleNotFoundException(NotFoundException ex) {
        ResponseModel<Map<String, Object>> body = this.handleException(ex);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseModel<Map<String, Object>>> handleBadRequestException(BadRequestException ex) {
        ResponseModel<Map<String, Object>> body = this.handleException(ex);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ResponseModel<Map<String, Object>>> handleForbiddenException(ForbiddenException ex) {
        ResponseModel<Map<String, Object>> body = this.handleException(ex);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<ResponseModel<Map<String, Object>>> handleNotAuthorizedException(NotAuthorizedException ex) {
        ResponseModel<Map<String, Object>> body = this.handleException(ex);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ResponseModel<Map<String, Object>>> handleNoHandlerFoundException(NotFoundException ex) {
        ResponseModel<Map<String, Object>> body = this.handleException(ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseModel<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        ResponseModel<Map<String, String>> response = new ResponseModel<>();
        response.setSuccess(false);
        response.setMessage("Validation failed");
        response.setData(errors);
        response.setTimestamp(java.time.LocalDateTime.now());

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseModel<Map<String, String>>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String dbMessage = e.getMostSpecificCause().getMessage();
        String userMessage = ConstraintMessageResolver.resolveMessage(dbMessage);

        ResponseModel<Map<String, String>> response = new ResponseModel<>();
        response.setSuccess(false);
        response.setMessage(userMessage);
        response.setData(null);
        response.setTimestamp(java.time.LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseModel<Map<String, Object>>> handleNoResourceFoundException(NoResourceFoundException ex) {
        ResponseModel<Map<String, Object>> body = new ResponseModel<>();
        body.setSuccess(false);
        body.setMessage("Resource not found");
        body.setData(null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseModel<Map<String, Object>>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        ResponseModel<Map<String, Object>> body = new ResponseModel<>();
        body.setSuccess(false);
        body.setMessage("Method not allowed");
        body.setData(null);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseModel<Map<String, String>>> handleGenericException(Exception ex) {
        ResponseModel<Map<String, String>> body = new ResponseModel<>();
        body.setSuccess(false);
        body.setMessage("Internal server error");
        body.setData(null);
        body.setTimestamp(java.time.LocalDateTime.now());
        ex.printStackTrace(); // Log the exception for debugging
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(TooManyRequestException.class)
    public ResponseEntity<ResponseModel<Map<String, Object>>> handleTooManyRequestException(TooManyRequestException ex) {
        ResponseModel<Map<String, Object>> body = this.handleException(ex);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ResponseModel<Map<String, Object>>> handleInvalidDataAccessApiUsageException(InvalidDataAccessApiUsageException ex) {
        ResponseModel<Map<String, Object>> body = this.handleException(ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private ResponseModel<Map<String, Object>> handleException(Exception ex) {
        ResponseModel<Map<String, Object>> response = new ResponseModel<>();
        response.setSuccess(false);
        response.setMessage(ex.getMessage());
        response.setData(null);
        return response;
    }
}
