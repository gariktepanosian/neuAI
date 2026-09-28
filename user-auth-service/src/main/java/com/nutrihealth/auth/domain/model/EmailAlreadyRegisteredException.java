package com.nutrihealth.auth.domain.model;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("An account with email " + email + " already exists");
    }
}
