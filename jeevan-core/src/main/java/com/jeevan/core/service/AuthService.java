package com.jeevan.core.service;

import com.jeevan.core.dto.request.LoginRequest;
import com.jeevan.core.dto.request.RegisterRequest;
import com.jeevan.core.dto.response.AuthResponse;
import com.jeevan.core.dto.response.UserResponse;
import com.jeevan.core.exception.EmailAlreadyExistsException;
import com.jeevan.core.exception.InvalidCredentialsException;
import com.jeevan.core.model.User;
import com.jeevan.core.model.enums.Role;
import com.jeevan.core.repository.UserRepository;
import com.jeevan.core.security.AppUserDetails;
import com.jeevan.core.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       EmailVerificationService emailVerificationService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * Creates a PATIENT account. When email verification is disabled (default
     * log-only path) the account is created already verified; when enabled it is
     * created unverified and a verification token is issued + emailed via the worker.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        boolean verificationEnabled = emailVerificationService.isEnabled();

        if (users.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(Role.PATIENT)
                .emailVerified(!verificationEnabled)
                .build();
        try {
            users.save(user);
        } catch (DataIntegrityViolationException e) {
            // Lost the race against a concurrent registration with the same email.
            throw new EmailAlreadyExistsException(request.email());
        }

        if (verificationEnabled) {
            emailVerificationService.issueAndPublish(user);
        }
        return UserResponse.from(user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }
        User user = ((AppUserDetails) authentication.getPrincipal()).getUser();
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, "Bearer", jwtService.getExpiryMinutes(), UserResponse.from(user));
    }
}
