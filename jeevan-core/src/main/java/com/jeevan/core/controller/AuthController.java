package com.jeevan.core.controller;

import com.jeevan.core.dto.request.LoginRequest;
import com.jeevan.core.dto.request.RegisterRequest;
import com.jeevan.core.dto.request.ResendVerificationRequest;
import com.jeevan.core.dto.response.AuthResponse;
import com.jeevan.core.dto.response.MessageResponse;
import com.jeevan.core.dto.response.UserResponse;
import com.jeevan.core.security.AppUserDetails;
import com.jeevan.core.service.AuthService;
import com.jeevan.core.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Current authenticated user — used by the frontend to rehydrate its session. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        return UserResponse.from(principal.getUser());
    }

    /** Verify an email via the token from the verification link (enabled-path only). */
    @GetMapping("/verify")
    public MessageResponse verify(@RequestParam String token) {
        emailVerificationService.verify(token);
        return new MessageResponse("VERIFIED", "Your email has been verified.");
    }

    /** Resend a verification email by address — unauthenticated and enumeration-safe. */
    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendByEmail(request.email());
        return new MessageResponse("SENT", "If an account exists for that email, a verification link has been sent.");
    }
}

