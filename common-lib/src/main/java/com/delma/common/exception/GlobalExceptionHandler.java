package com.delma.common.exception;


import com.delma.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;        // ← ADD THIS
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Central exception handler inherited by ALL Delma microservices.
 *
 * LESSON — How this works across services:
 * Each service adds common-lib as a dependency.
 * Spring Boot's component scan picks up @RestControllerAdvice automatically
 * as long as com.delma.common is within the scan path.
 *
 * To enable scanning in each service, add this to its main application class:
 *   @SpringBootApplication(scanBasePackages = {"com.delma.yourservice", "com.delma.common"})
 *
 * Exception → HTTP status mapping:
 *   ResourceNotFoundException  → 404 Not Found
 *   ConflictException          → 409 Conflict
 *   BadRequestException        → 400 Bad Request
 *   UnauthorizedException      → 403 Forbidden
 *   MethodArgumentNotValidException → 400 (with field-level detail)
 *   Exception (catch-all)      → 500 Internal Server Error
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), "RESOURCE_NOT_FOUND"));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex){
        log.warn("Conflic :{}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ex.getMessage(),"CONFLICT"));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ex.getMessage(), "FORBIDDEN"));
    }

    /*
    * Handle @Valid failures - Returns which fields failed and why
    * */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String,String>>> handleValidation(MethodArgumentNotValidException ex){
            Map<String,String> errors = new HashMap<>();
            for(FieldError fieldError : ex.getBindingResult().getFieldErrors()){
                errors.put(fieldError.getField(),fieldError.getDefaultMessage());
            }

            log.warn("Validation failed: {}",errors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.success(errors,"Validation failed"));
    }




    /*
    * Safety net - Catches anything not handled above
    *  NEVER return ex.getMessage() here — it can leak DB names,
        class names, SQL queries to the client. Log it, hide it.
    * */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex){
        log.error("Unexpected error: ",ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(
                        "An unexpected error occurred. Please try again later...",
                        "INTERNAL_ERROR"
                ));
    }

    // NEVER return ex.getMessage() here — it can leak DB names,
    // class names, SQL queries to the client. Log it, hide it.


}
