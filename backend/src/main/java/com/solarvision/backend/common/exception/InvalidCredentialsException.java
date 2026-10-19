package com.solarvision.backend.common.exception;

/** Thrown when a login fails: unknown email, wrong password or an inactive account. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}