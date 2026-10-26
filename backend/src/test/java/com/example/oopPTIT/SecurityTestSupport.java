package com.example.oopPTIT;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.SecureRandom;
import java.util.Base64;

@ActiveProfiles("test")
public abstract class SecurityTestSupport {
    private static final String SECRET = randomSecret();

    @DynamicPropertySource
    static void securityProperties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> SECRET);
        registry.add("app.jwt.issuer", () -> "test-issuer");
        registry.add("app.jwt.audience", () -> "test-api");
        registry.add("app.jwt.ttl", () -> "15m");
        registry.add("app.cors.allowed-origins", () -> "http://localhost:5173");
        registry.add("app.docs.enabled", () -> "false");
        registry.add("springdoc.api-docs.enabled", () -> "false");
        registry.add("springdoc.swagger-ui.enabled", () -> "false");
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
