package com.delma.common.exception;
/**
 * Throw when the request is valid but conflicts with existing data state.
 * GlobalExceptionHandler maps this → HTTP 409 Conflict.
 *
 * Usage:
 *   if (doctorRepository.existsByUserIdAndStatus(userId, PENDING)) {
 *       throw new ConflictException("Pending application already exists for user: " + userId);
 *   }
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message){
        super(message);
    }
}
