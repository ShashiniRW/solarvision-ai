package com.solarvision.backend.common.exception;

/**
 * The request is valid, but it clashes with the current state of the data
 * (duplicate value, or an action that is not allowed in the current status).
 * Mapped to HTTP 409 Conflict.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}