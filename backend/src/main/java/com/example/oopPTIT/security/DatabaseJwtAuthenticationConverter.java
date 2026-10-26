package com.example.oopPTIT.security;

import com.example.oopPTIT.repository.UserRepository;
import com.example.oopPTIT.util.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long id;
        try {
            id = Long.valueOf(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new InvalidBearerTokenException("Invalid access token");
        }
        var user = userRepository.findById(id)
                .orElseThrow(() -> new InvalidBearerTokenException("Invalid access token"));
        if (user.getStatus() != Status.ACTIVE
                || !user.getUsername().equals(jwt.getClaims().get("username"))
                || !List.of(user.getRole().name()).equals(jwt.getClaims().get("roles"))
                || !id.toString().equals(String.valueOf(jwt.getClaims().get("userId")))) {
            throw new InvalidBearerTokenException("Invalid access token");
        }
        return new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())), user.getUsername());
    }
}
