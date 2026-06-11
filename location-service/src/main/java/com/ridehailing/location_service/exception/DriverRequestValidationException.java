package com.ridehailing.location_service.exception;

public class DriverRequestValidationException extends RuntimeException {
    public DriverRequestValidationException(String message) {
        super(message);
    }
}
