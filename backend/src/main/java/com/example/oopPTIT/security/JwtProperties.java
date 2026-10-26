package com.example.oopPTIT.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(@NotBlank String secret, @NotBlank String issuer,
                            @NotBlank String audience, @NotNull Duration ttl) {
    @Override
    public String toString() {
        return "JwtProperties[secret=REDACTED, issuer=" + issuer + ", audience=" + audience + ", ttl=" + ttl + "]";
    }
}
