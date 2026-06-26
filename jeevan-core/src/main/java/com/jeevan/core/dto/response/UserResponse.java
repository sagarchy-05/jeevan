package com.jeevan.core.dto.response;

import com.jeevan.core.model.User;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String role,
        boolean emailVerified
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.isEmailVerified());
    }
}
