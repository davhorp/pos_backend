package com.school.app.audit;

import com.school.app.entity.SystemAuditLog;
import com.school.app.entity.User;
import com.school.app.repository.SystemAuditLogRepository;
import com.school.app.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final SystemAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @AfterReturning(value = "@annotation(auditable)", returning = "result")
    public void logAuditActivity(JoinPoint joinPoint, Auditable auditable, Object result) {
        log.debug("Iniciando captura de auditoría para la acción: {}", auditable.action());

        SystemAuditLog auditLog = new SystemAuditLog();
        auditLog.setActionType(auditable.action());
        auditLog.setEntityName(auditable.entityName());

        // 1. Obtener el usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            Optional<User> userOptional = userRepository.findByUsername(username);

            userOptional.ifPresent(user -> {
                auditLog.setUser(user);
                log.debug("Usuario vinculado a la auditoría: {}", username);
            });
        } else {
            // Un WARN es útil aquí, por ejemplo, si un endpoint público dispara una acción auditable
            log.warn("No se detectó un usuario autenticado para la acción: {}", auditable.action());
        }

        // 2. Obtener la IP
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ip = getClientIp(request);
            auditLog.setIpAddress(ip);
            log.debug("IP capturada: {}", ip);
        }

        // 3. Capturar el ID de la entidad afectada mediante Reflection
        if (result != null) {
            try {
                UUID entityId = (UUID) result.getClass().getMethod("getId").invoke(result);
                auditLog.setEntityId(entityId);
                log.debug("Entity ID extraído exitosamente: {}", entityId);

            } catch (NoSuchMethodException e) {
                // Usamos TRACE o DEBUG porque no es un error real, solo significa que
                // el método no retorna un objeto con getId() (ej. devuelve un boolean o String)
                log.trace("El objeto retornado tipo {} no posee un método getId(). Se omite el entityId.",
                        result.getClass().getSimpleName());
            } catch (Exception e) {
                // ERROR sí es pertinente si hay un fallo de acceso o seguridad en Java Reflection
                log.error("Fallo inesperado al intentar extraer el ID por Reflection del objeto: {}",
                        result.getClass().getSimpleName(), e);
            }
        }

        // 4. Guardar en Base de Datos
        try {
            auditLogRepository.save(auditLog);
            log.info("Registro de auditoría guardado exitosamente -> Acción: {} | Entidad: {}",
                    auditable.action(), auditable.entityName());
        } catch (Exception e) {
            // Un fallo al guardar la auditoría no hace rollback de la transacción principal por defecto,
            // pero es un error crítico para el área de seguridad que debe alertarse.
            log.error("Fallo crítico al insertar el registro de auditoría en la base de datos.", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        }
        // Devuelve la primera IP en caso de pasar por múltiples proxies
        return xfHeader.split(",")[0];
    }
}
