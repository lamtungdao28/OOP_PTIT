package com.example.oopPTIT.config;

import com.example.oopPTIT.security.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTests {
    private final SecurityConfig config = new SecurityConfig();

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid-base64!", "c2hvcnQ="})
    void rejectsMissingMalformedOrWeakSecrets(String secret) {
        assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties(secret, "issuer", "api", Duration.ofMinutes(15))))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 3601})
    void rejectsUnsafeTokenLifetimes(long seconds) {
        assertThatThrownBy(() -> config.jwtSecretKey(new JwtProperties(secret(), "issuer", "api", Duration.ofSeconds(seconds))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsWildcardCorsOrigins() {
        assertThatThrownBy(() -> config.corsConfigurationSource("https://*.example.test"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsRandomKeyAndRedactsItFromDiagnostics() {
        String secret = secret();
        var properties = new JwtProperties(secret, "issuer", "api", Duration.ofMinutes(15));
        assertThat(config.jwtSecretKey(properties).getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(properties.toString()).doesNotContain(secret);
    }

    private String secret() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
