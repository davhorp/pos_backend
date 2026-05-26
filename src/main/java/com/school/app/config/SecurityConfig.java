package com.school.app.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("AUDITORÍA (SISTEMA) - Inicializando la cadena de seguridad (SecurityFilterChain)...");

        http
                // 1. Configuración CORS y desactivación de CSRF
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Reglas de Autorización de Rutas (Endpoints)
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers("api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/banks/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/simulations/**").permitAll()

                        // Endpoints protegidos
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/documents/**").hasAnyRole("ADMIN", "BROKER", "CLIENT")
                        .requestMatchers("/api/pos/**").authenticated()
                        // Cualquier otra petición debe estar autenticada
                        .anyRequest().authenticated()
                )

                // 3. Manejo de Excepciones de Seguridad (Para Auditoría en Tiempo de Ejecución)
                .exceptionHandling(exceptions -> exceptions
                        // Atrapa el HTTP 401 (No autenticado / Token faltante o inválido en rutas protegidas)
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("AUDITORÍA (SEGURIDAD) - Acceso NO AUTORIZADO (401) intentado en URI: {} desde IP: {}. Motivo: {}",
                                    request.getRequestURI(), request.getRemoteAddr(), authException.getMessage());
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("Full authentication is required to access this resource");
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No autorizado para acceder a este recurso");
                        })
                        // Atrapa el HTTP 403 (Autenticado, pero sin el Rol necesario)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("AUDITORÍA (SEGURIDAD) - Acceso DENEGADO (403) en URI: {} desde IP: {}. El usuario no tiene los permisos suficientes.",
                                    request.getRequestURI(), request.getRemoteAddr());
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "No tienes permisos para realizar esta acción");
                        })
                )

                // 4. Configuración de Sesión Stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 5. Proveedor de Autenticación y Filtros Personalizados
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("AUDITORÍA (SISTEMA) - Cadena de seguridad construida e implementada con éxito.");
        return http.build();
    }

    // Configuración estricta de CORS para conectar con Angular
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("AUDITORÍA (SISTEMA) - Cargando políticas CORS estrictas. Orígenes permitidos: http://localhost:4200");

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
