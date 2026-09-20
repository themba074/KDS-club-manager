package com.kds.backend.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;

@Component
@Profile("staging")
public class StagingConfigurationValidator implements InitializingBean {
    static final String DEVELOPMENT_JWT_SECRET =
            "kds-local-development-signing-key-change-before-production-2026";
    static final String DEVELOPMENT_DATABASE_PASSWORD = "kds_dev_password";
    private final Environment configuration;

    public StagingConfigurationValidator(Environment configuration) {
        this.configuration = configuration;
    }

    @Override
    public void afterPropertiesSet() {
        rejectValue("app.auth.jwt-secret", DEVELOPMENT_JWT_SECRET);
        rejectValue("spring.datasource.password", DEVELOPMENT_DATABASE_PASSWORD);
        requireFalse("app.auth.development-reset-delivery");
        requireFalse("app.members.development-invitation-delivery");
        requireTrue("app.auth.refresh-cookie-secure");
        requireHttps("app.auth.frontend-url");

        Arrays.stream(required("app.auth.allowed-origins").split(","))
                .map(String::trim)
                .forEach(origin -> requireHttps("app.auth.allowed-origins", origin));

        requireValue("app.storage.provider", "supabase");
        requireHttps("app.storage.supabase.url");
        required("app.storage.supabase.service-role-key");
        required("app.storage.supabase.bucket");

        requireValue("app.notifications.email-provider", "smtp");
        required("app.notifications.from-address");
        required("spring.mail.host");
        required("spring.mail.username");
        required("spring.mail.password");
        requireTrue("spring.mail.properties.mail.smtp.auth");
        requireTrue("spring.mail.properties.mail.smtp.starttls.enable");
    }

    private void rejectValue(String property, String rejectedValue) {
        if (rejectedValue.equals(required(property))) {
            throw invalid(property, "must not use the repository-known development value");
        }
    }

    private void requireTrue(String property) {
        if (!Boolean.parseBoolean(required(property))) {
            throw invalid(property, "must be true in the staging profile");
        }
    }

    private void requireFalse(String property) {
        if (Boolean.parseBoolean(required(property))) {
            throw invalid(property, "must be false in the staging profile");
        }
    }

    private void requireValue(String property, String expectedValue) {
        if (!expectedValue.equalsIgnoreCase(required(property))) {
            throw invalid(property, "must be " + expectedValue + " in the staging profile");
        }
    }

    private void requireHttps(String property) {
        requireHttps(property, required(property));
    }

    private void requireHttps(String property, String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw invalid(property, "must contain a valid HTTPS URL");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw invalid(property, "must contain a valid HTTPS URL");
        }
    }

    private String required(String property) {
        String value = configuration.getProperty(property);
        if (value == null || value.isBlank()) {
            throw invalid(property, "is required in the staging profile");
        }
        return value.trim();
    }

    private IllegalStateException invalid(String property, String message) {
        return new IllegalStateException("Unsafe staging configuration: " + property + " " + message);
    }
}
