package com.delma.aiservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class AiExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        // Log full stack trace so we can see exactly which line threw NPE
        log.error("Exception in aiservice: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError()
                .body(Map.of(
                        "success", false,
                        "error", ex.getClass().getSimpleName(),
                        "message", ex.getMessage() != null ? ex.getMessage() : "null"
                ));
    }
}