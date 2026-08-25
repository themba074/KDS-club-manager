package com.kds.backend.identity.application;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() { super("An account with this email already exists."); }
}
