package com.jeevan.core.dto.response;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMinutes,
        UserResponse user
) {
}
