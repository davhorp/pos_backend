package com.school.app.config;

import com.school.app.exceptions.ExpiredJwtException;
import com.school.app.services.auth.JwtService;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SignatureException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Verificar si el header existe
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Usamos TRACE para no inundar los logs en rutas públicas (como el login o el simulador)
            log.trace("AUDITORÍA - Petición sin token JWT o formato incorrecto. URI: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }
        jwt = authHeader.substring(7);
        try {
            // 2. Extraer el token
            userEmail = jwtService.extractUsername(jwt);
            // 3. Si tenemos email y no hay nadie autenticado aún
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                    // 4. Validar el token
                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        // 5. Actualizar el contexto
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.info("Usuario: {} | Autoridades: {}",
                                SecurityContextHolder.getContext().getAuthentication().getName(),
                                SecurityContextHolder.getContext().getAuthentication().getAuthorities());
                        // Usamos DEBUG para saber que alguien entró exitosamente, sin saturar la consola en producción
                        log.info("AUDITORÍA - Acceso concedido a recurso protegido. Usuario: {}, URI: {}", userEmail, request.getRequestURI());
                    } else {
                        // El token es estructuralmente correcto pero belongsTo otro usuario (muy raro, posible spoofing)
                        log.warn("AUDITORÍA (ALERTA) - Token JWT inválido detectado para el usuario: {}. IP: {}", userEmail, request.getRemoteAddr());
                    }
                } catch (UsernameNotFoundException e) {
                    log.error("AUDITORÍA (CRÍTICO) - El token contiene un email ({}) que ya no existe en la base de datos.", userEmail);
                }
            }
            // --- MANEJO DE EXCEPCIONES DE SEGURIDAD JJWT ---
        } catch (ExpiredJwtException e) {
            log.warn("AUDITORÍA - Intento de acceso con token expirado. URI: {}, IP: {}", request.getRequestURI(), request.getRemoteAddr());
            // Opcional: Aquí podrías alterar la respuesta para enviar un código custom indicando al frontend que debe usar el Refresh Token
        } catch (MalformedJwtException e) {
            log.error("AUDITORÍA (PELIGRO) - Token JWT alterado o malformado detectado. Posible intento de falsificación de identidad. IP: {}", request.getRemoteAddr());
        } catch (Exception e) {
            log.error("AUDITORÍA - Error inesperado procesando el JWT de la IP {}: {}", request.getRemoteAddr(), e.getMessage());
        }
        // Continuar con la cadena (Si falló algo arriba, el contexto sigue nulo y Spring lanzará 403 Forbidden)
        filterChain.doFilter(request, response);
    }
}
