package com.school.app.services.auth;

import com.school.app.audit.Auditable;
import com.school.app.dto.requets.AuthRequest;
import com.school.app.dto.response.AuthResponse;
import com.school.app.entity.SystemAuditLog;
import com.school.app.entity.User;
import com.school.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    // Usamos el Aspecto (AOP) que creamos antes para guardar el log en SystemAuditLog
    @Auditable(action = SystemAuditLog.AuditAction.LOGIN_SUCCESS, entityName = "USER")
    public AuthResponse login(AuthRequest request) {
        log.info("Intento de login para usuario: {}", request.username());

        // 1. Esto valida la contraseña contra el hash de la BD automáticamente.
        // Si la contraseña es incorrecta, lanza BadCredentialsException (que ya capturamos en el GlobalExceptionHandler).
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        // 2. Establecemos la autenticación en el contexto para que nuestro AOP sepa quién se logueó
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Cargamos los detalles y generamos el Token
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String jwtToken = jwtService.generateToken(userDetails);

        log.info("Login exitoso para usuario: {}", request.username());
        User usr = userRepository.findByUsername(request.username()).get();
        // 4. Retornamos el DTO
        return new AuthResponse(jwtToken, usr.getFullName(), usr.getEmail(), usr.getRole().name());
    }
}
