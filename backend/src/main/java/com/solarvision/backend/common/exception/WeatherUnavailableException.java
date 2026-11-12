package com.solarvision.backend.common.exception;

/** The weather provider could not be reached or returned unusable data (becomes HTTP 503). */
public class WeatherUnavailableException extends RuntimeException {

    public WeatherUnavailableException(String message) {
        super(message);
    }

    public WeatherUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
