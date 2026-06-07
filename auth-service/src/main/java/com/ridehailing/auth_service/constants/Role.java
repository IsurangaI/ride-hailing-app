package com.ridehailing.auth_service.constants;

public enum Role {
    PASSENGER("PASSENGER"),
    DRIVER("DRIVER"),
    ADMIN("ADMIN");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}