package com.kds.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StagingConfigurationValidatorTests {
    @Test
    void acceptsProductionShapedConfiguration() {
        assertDoesNotThrow(() -> new StagingConfigurationValidator(secureEnvironment()).afterPropertiesSet());
    }

    @Test
    void rejectsRepositoryKnownDevelopmentSecrets() {
        var environment = secureEnvironment()
                .withProperty("app.auth.jwt-secret", StagingConfigurationValidator.DEVELOPMENT_JWT_SECRET);

        assertThrows(IllegalStateException.class,
                () -> new StagingConfigurationValidator(environment).afterPropertiesSet());
    }

    @Test
    void rejectsInsecureBrowserOrigins() {
        var environment = secureEnvironment()
                .withProperty("app.auth.allowed-origins", "http://staging.example.test");

        assertThrows(IllegalStateException.class,
                () -> new StagingConfigurationValidator(environment).afterPropertiesSet());
    }

    private MockEnvironment secureEnvironment() {
        return new MockEnvironment()
                .withProperty("app.auth.jwt-secret", "a-unique-staging-signing-secret-with-at-least-32-bytes")
                .withProperty("spring.datasource.password", "a-unique-staging-database-password")
                .withProperty("app.auth.development-reset-delivery", "false")
                .withProperty("app.members.development-invitation-delivery", "false")
                .withProperty("app.auth.refresh-cookie-secure", "true")
                .withProperty("app.auth.frontend-url", "https://staging.example.test")
                .withProperty("app.auth.allowed-origins", "https://staging.example.test")
                .withProperty("app.storage.provider", "supabase")
                .withProperty("app.storage.supabase.url", "https://storage.example.test")
                .withProperty("app.storage.supabase.service-role-key", "storage-secret")
                .withProperty("app.storage.supabase.bucket", "kds-private")
                .withProperty("app.notifications.email-provider", "smtp")
                .withProperty("app.notifications.from-address", "no-reply@example.test")
                .withProperty("spring.mail.host", "smtp.example.test")
                .withProperty("spring.mail.username", "smtp-user")
                .withProperty("spring.mail.password", "smtp-password")
                .withProperty("spring.mail.properties.mail.smtp.auth", "true")
                .withProperty("spring.mail.properties.mail.smtp.starttls.enable", "true");
    }
}
