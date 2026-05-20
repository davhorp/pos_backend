package com.school.app.config;

import com.school.app.entity.User;
import com.school.app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
public class DataSeedConfig {

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Verifica si el usuario administrador ya existe
            if (!userRepository.existsByUsername("davhoAdmin")) {

                User admin = User.builder()
                        .username("davhoAdmin")
                        .email("davhorp90@gmail.com")
                        // Aquí Spring genera un hash BCrypt perfecto y compatible
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .role(User.Role.ADMIN)
                        .isActive(true)
                        .lastName("Reyes")
                        .firstName("Jonathan")
                        .build();
                userRepository.save(admin);
                log.info("✅ STARTUP: Usuario ADMIN creado por defecto con contraseña: 'admin123'");
            }
        };
    }
}
