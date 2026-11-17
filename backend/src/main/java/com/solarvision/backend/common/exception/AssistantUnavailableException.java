package com.solarvision.backend.common.exception;

/** The AI assistant cannot answer right now: no key, AI service down, too slow, or an error. */
public class AssistantUnavailableException extends RuntimeException {

    public AssistantUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
