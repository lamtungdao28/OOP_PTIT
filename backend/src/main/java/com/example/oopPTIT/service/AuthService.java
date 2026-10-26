package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.request.LoginRequest;
import com.example.oopPTIT.dto.response.CurrentUserResponse;
import com.example.oopPTIT.dto.response.LoginResponse;
import com.example.oopPTIT.repository.UserRepository;
import com.example.oopPTIT.security.AuthenticatedUser;
import com.example.oopPTIT.security.CompatiblePasswordEncoder;
import com.example.oopPTIT.security.CurrentUser;
import com.example.oopPTIT.security.JwtProperties;
import com.example.oopPTIT.util.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final CompatiblePasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final CurrentUser currentUser;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername().strip(), request.getPassword()));
        var principal = (AuthenticatedUser) authentication.getPrincipal();
        var user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (user.getStatus() != Status.ACTIVE) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (passwordEncoder.needsUpgrade(request.getPassword(), user.getPassword())) {
            userRepository.upgradePassword(user.getId(), user.getPassword(),
                    passwordEncoder.encode(request.getPassword()), LocalDateTime.now());
        }
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.issuer())
                .subject(user.getId().toString()).audience(List.of(properties.audience()))
                .issuedAt(now).notBefore(now).expiresAt(now.plus(properties.ttl())).id(UUID.randomUUID().toString())
                .claim("userId", user.getId()).claim("username", user.getUsername())
                .claim("roles", List.of(user.getRole().name())).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", properties.ttl().toSeconds(), CurrentUserResponse.from(user));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public CurrentUserResponse me() {
        return userRepository.findById(currentUser.id()).map(CurrentUserResponse::from)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    }
}
