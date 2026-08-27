package com.kds.backend.identity.api;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordValidationTests {
    @Test void registrationAndResetAcceptEightCharactersButRejectSeven() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new AuthDtos.RegisterRequest("test@example.com", "abcdefgh")).isEmpty());
            assertFalse(validator.validate(new AuthDtos.RegisterRequest("test@example.com", "abcdefg")).isEmpty());
            assertTrue(validator.validate(new AuthDtos.PasswordResetConfirmRequest("token", "abcdefgh")).isEmpty());
            assertFalse(validator.validate(new AuthDtos.PasswordResetConfirmRequest("token", "abcdefg")).isEmpty());
        }
    }
}
