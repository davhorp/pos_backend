package com.school.app.dto.response;

import com.school.app.entity.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        User.Role role,
        boolean isActive
) {
}
