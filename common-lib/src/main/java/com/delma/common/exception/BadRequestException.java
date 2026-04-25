package com.delma.common.exception;

/**
 * Throw when request data is logically invalid beyond basic @Valid checks.
 * GlobalExceptionHandler maps this → HTTP 400 Bad Request.
 *
 * Usage:
 *   if (appointment.getStartTime().isAfter(appointment.getEndTime())) {
 *       throw new BadRequestException("Start time cannot be after end time");
 *   }
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message){
        super(message);
    }
}
