package com.delma.common.exception;


/**
 * Throw when a specific entity by ID doesn't exist.
 * GlobalExceptionHandler maps this → HTTP 404 Not Found.
 *
 * Usage:
 *   doctorRepository.findById(id)
 *       .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));
 */
public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String message){
        super(message);
    }
}
