package com.school.app.controllers.user;

import com.school.app.dto.requets.CreateUserRequest;
import com.school.app.dto.response.UserResponse;
import com.school.app.services.auth.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final RegisterService registerService;

    // @PreAuthorize exige que el token JWT del solicitante tenga el rol ADMIN
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        UserResponse response = registerService.createUser(request);
        // Retornamos HTTP 201 (Created)
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
