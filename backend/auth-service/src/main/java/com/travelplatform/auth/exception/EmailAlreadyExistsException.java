package com.travelplatform.auth.exception;

/**
 * Thrown by {@link com.travelplatform.auth.service.AuthServiceImpl}
 * when a registration request uses an email that is already in the database.
 * Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Email is already in use: " + email);
    }
}
