package com.example.oopPTIT.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class CompatiblePasswordEncoder implements PasswordEncoder {
    private static final Pattern LEGACY_HASH = Pattern.compile("[0-9a-fA-F]{96}");
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);
    private final PasswordEncoder legacy = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @Override
    public String encode(CharSequence rawPassword) {
        if (!fitsBcrypt(rawPassword)) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes");
        }
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) return false;
        if (LEGACY_HASH.matcher(encodedPassword).matches()) {
            return legacy.matches(rawPassword, encodedPassword);
        }
        return fitsBcrypt(rawPassword) && bcrypt.matches(rawPassword, encodedPassword);
    }

    // Upgrade after authentication, only when BCrypt can represent the entire password.
    public boolean needsUpgrade(CharSequence rawPassword, String hash) {
        return fitsBcrypt(rawPassword) && (LEGACY_HASH.matcher(hash).matches() || bcrypt.upgradeEncoding(hash));
    }

    private boolean fitsBcrypt(CharSequence password) {
        return password.toString().getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
