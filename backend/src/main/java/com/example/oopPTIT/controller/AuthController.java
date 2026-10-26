package com.example.oopPTIT.controller;

import com.example.oopPTIT.dto.request.LoginRequest;
import com.example.oopPTIT.dto.response.CurrentUserResponse;
import com.example.oopPTIT.dto.response.LoginResponse;
import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    public ResponseData<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return new ResponseData<>(200, "Login successful", authService.login(request));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseData<CurrentUserResponse> me() {
        return new ResponseData<>(200, "Current user retrieved successfully", authService.me());
    }
}
