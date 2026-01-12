package org.personal.simulator.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the simulator.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SimulatorException.class)
    public ResponseEntity<Map<String, Object>> handleSimulatorException(SimulatorException ex) {
        log.error("Simulator error: {} ({})", ex.getMessage(), ex.getErrorCode());

        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", ex.getErrorCode());
        error.put("message", ex.getMessage());
        error.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", "VALIDATION_ERROR");
        error.put("message", "Request validation failed");
        error.put("fieldErrors", fieldErrors);
        error.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", "INVALID_ARGUMENT");
        error.put("message", ex.getMessage());
        error.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", "INTERNAL_ERROR");
        error.put("message", "An unexpected error occurred");
        error.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
