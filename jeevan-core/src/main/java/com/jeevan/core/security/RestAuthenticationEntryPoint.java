package com.jeevan.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevan.core.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns the shared {@link ApiError} envelope (instead of Spring's default HTML)
 * when an unauthenticated request hits a protected endpoint. The error code is
 * refined to TOKEN_EXPIRED / INVALID_TOKEN when {@link JwtAuthFilter} recorded one.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object jwtError = request.getAttribute(JwtAuthFilter.JWT_ERROR_ATTRIBUTE);
        String code = jwtError != null ? jwtError.toString() : "UNAUTHORIZED";
        String message = "TOKEN_EXPIRED".equals(code)
                ? "Your session has expired. Please log in again."
                : "Authentication is required to access this resource.";

        ApiError body = ApiError.of(HttpStatus.UNAUTHORIZED.value(), code, message, request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
