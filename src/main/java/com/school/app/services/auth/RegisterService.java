package com.school.app.services.auth;

import com.school.app.audit.Auditable;
import com.school.app.dto.requets.CreateUserRequest;
import com.school.app.dto.response.UserResponse;
import com.school.app.entity.SystemAuditLog;
import com.school.app.entity.User;
import com.school.app.exceptions.UserAlreadyExistsException;
import com.school.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Service
@Slf4j
public class RegisterService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // La anotación Auditable registrará automáticamente quién creó a este usuario
    @Auditable(action = SystemAuditLog.AuditAction.USER_CREATED, entityName = "USER")
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Intentando crear un nuevo usuario: {}", request.username());

        // 1. Validar que el usuario no exista
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("El nombre de usuario '" + request.username() + "' ya está registrado.");
        }
        // 2. Construir la entidad encriptando la contraseña
        User newUser = User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .isActive(true)
                .build();
        // 3. Guardar en la base de datos
        User savedUser = userRepository.save(newUser);
        log.info("Usuario creado exitosamente: {} con rol {}", savedUser.getUsername(), savedUser.getRole());
        // 4. Mapear a DTO para la respuesta
        return new UserResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole(),
                savedUser.getIsActive()
        );
    }
}
