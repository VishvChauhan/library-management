package com.example.library.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ApiError> handleBookNotFound(BookNotFoundException exception, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateIsbnException.class)
    public ResponseEntity<ApiError> handleDuplicateIsbn(DuplicateIsbnException exception, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(BookNotAvailableException.class)
    public ResponseEntity<ApiError> handleBookNotAvailable(BookNotAvailableException exception, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleInvalidPayload(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Request body is invalid or malformed", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ApiError> buildError(HttpStatus status,
                                                String message,
                                                HttpServletRequest request,
                                                Map<String, String> validationErrors) {
        ApiError error = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                validationErrors
        );
        return ResponseEntity.status(status).body(error);
    }

    public record ApiError(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
    }
}

