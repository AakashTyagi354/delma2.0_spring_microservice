package com.delma.common.exception;


/**
 * Throw when a user is authenticated but not permitted to perform an action.
 * GlobalExceptionHandler maps this → HTTP 403 Forbidden.
 *
 * Usage:
 *   if (!doctor.getUserId().equals(requestingUserId)) {
 *       throw new UnauthorizedException("You can only update your own profile");
 *   }
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message){
        super(message);
    }
}
