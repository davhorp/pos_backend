package com.school.app.dto.requets;

import com.school.app.entity.User;

public record CreateUserRequest(
        String username,
        String password,
        User.Role role
) {
}
