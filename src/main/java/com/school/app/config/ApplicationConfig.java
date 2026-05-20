package com.school.app.config;

import com.school.app.repository.UserRepository;
import com.school.app.services.auth.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final CustomUserDetailsService userDetailsService;

    // 1. Declaramos el encriptador de contraseñas (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. Le decimos a Spring Security cómo proveer la autenticación
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // Le pasamos nuestro servicio que busca en la BD
        authProvider.setUserDetailsService(userDetailsService);
        // Le pasamos el encriptador para que compare las contraseñas
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // 3. Exponemos el AuthenticationManager para usarlo en nuestro AuthService
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
