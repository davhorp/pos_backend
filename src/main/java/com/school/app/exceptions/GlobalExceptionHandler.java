package com.school.app.exceptions;

import com.school.app.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@Slf4j // Inyecta el logger que escribirá en el archivo físico
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Manejo de errores de negocio (Ej: Stock Insuficiente)
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessExceptions(InsufficientStockException ex, HttpServletRequest request) {
        // Registramos en el log como un WARN porque es un flujo de negocio esperado, no un fallo del servidor
        log.warn("Regla de negocio no cumplida: {} | URL: {}", ex.getMessage(), request.getRequestURI());

        ApiErrorResponse error = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.CONFLICT.value(), // 409 Conflict es semánticamente correcto para problemas de estado/stock
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // 2. Manejo de errores de Autenticación (Credenciales incorrectas)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthExceptions(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Intento de login fallido desde IP: {} | URL: {}", request.getRemoteAddr(), request.getRequestURI());

        ApiErrorResponse error = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(), // 401
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Usuario o contraseña incorrectos",
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // 3. Manejo de errores de Autorización (Ej: Vendedor intentando ver reportes Admin)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado. URL: {}", request.getRequestURI());

        ApiErrorResponse error = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.FORBIDDEN.value(), // 403
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "No tienes permisos para realizar esta acción",
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // 4. Paracaídas Final: Cualquier error inesperado (NullPointer, Base de Datos caída, etc)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAllOtherExceptions(Exception ex, HttpServletRequest request) {
        // Este sí es un ERROR crítico. Imprimimos el stacktrace completo (ex) en el archivo de log
        log.error("Error crítico no controlado en la aplicación | URL: {}", request.getRequestURI(), ex);

        ApiErrorResponse error = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(), // 500
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ocurrió un error interno en el servidor. Contacte a soporte.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex, HttpServletRequest request) {
        // Este sí es un ERROR crítico. Imprimimos el stacktrace completo (ex) en el archivo de log
        log.error("Error crítico no controlado en la aplicación | URL: {}", request.getRequestURI(), ex);

        ApiErrorResponse error = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.CONFLICT.value(), // 500
                HttpStatus.CONFLICT.getReasonPhrase(),
                "Ocurrió un error interno en el servidor. Contacte a soporte.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
