package com.school.app.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.app.entity.SystemAuditLog;
import com.school.app.entity.User;
import com.school.app.repository.SystemAuditLogRepository;
import com.school.app.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final SystemAuditLogRepository auditRepository;
    private final UserRepository userRepository; // Para buscar la entidad User
    private final ObjectMapper objectMapper;
    /**
     * Usamos @Around para envolver el método.
     * Pasar la anotación 'auditable' en los parámetros nos permite leer sus valores (action, entityName) directamente.
     */
    @Around(value = "@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long startTime = System.currentTimeMillis();
        boolean exito = true;
        String mensajeError = null;
        Object result = null;

        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        try {
            // 1. Ejecutamos el método original
            result = joinPoint.proceed();
            return result;

        } catch (Throwable e) {
            // 2. Si hay error, lo capturamos para el JSON de auditoría
            exito = false;
            mensajeError = e.getMessage();
            throw e;

        } finally {
            // 3. Bloque de construcción de la auditoría (Garantizado)
            try {
                // --- A. Extraer IP ---
                String ipAddress = "SISTEMA_INTERNO";
                RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
                    ipAddress = request.getHeader("X-Forwarded-For");
                    if (ipAddress == null || ipAddress.isEmpty()) {
                        ipAddress = request.getRemoteAddr();
                    }
                }

                // --- B. Extraer Usuario Autenticado ---
                User currentUser = null;
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                    // Opcional: Podrías guardar el User en la sesión para evitar ir a la BD,
                    // pero esta es la forma más segura si necesitas la relación @ManyToOne real.
                    currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
                }

                // --- C. Construir el JSON de detalles (detailsPayload) ---
                Map<String, Object> payload = new HashMap<>();
                payload.put("modulo", className + "." + methodName);
                payload.put("tiempo_ejecucion_ms", System.currentTimeMillis() - startTime);
                payload.put("exito", exito);
                if (!exito) {
                    payload.put("error", mensajeError);
                }

                String jsonPayload = objectMapper.writeValueAsString(payload);

                // --- D. Intentar extraer el Entity ID del resultado (Opcional pero muy útil) ---
                UUID entityId = tryExtractIdFromResult(result);

                // --- E. Guardar en Base de Datos usando tu Entidad ---
                SystemAuditLog auditLog = SystemAuditLog.builder()
                        .actionType(auditable.action())
                        .entityName(auditable.entityName())
                        .ipAddress(ipAddress)
                        .user(currentUser)
                        .detailsPayload(jsonPayload)
                        .entityId(entityId)
                        .build();

                auditRepository.save(auditLog);

            } catch (Exception ex) {
                log.error("Error catastrófico guardando auditoría (Aspecto): {}", ex.getMessage());
            }
        }
    }

    /**
     * Método helper usando Reflection.
     * Intenta sacar el UUID dinámicamente si el método original devolvió
     * una Entidad o DTO que tenga el método getId() o id() (Records).
     */
    private UUID tryExtractIdFromResult(Object result) {
        if (result == null) return null;
        try {
            // Intenta para Clases tradicionales (DTOs/Entidades con @Getter)
            Method getIdMethod = result.getClass().getMethod("getId");
            Object idObj = getIdMethod.invoke(result);
            if (idObj instanceof UUID) return (UUID) idObj;
        } catch (Exception ignored) {
            try {
                // Intenta para Records de Java 14+ (tienen un método id() directo)
                Method idMethod = result.getClass().getMethod("id");
                Object idObj = idMethod.invoke(result);
                if (idObj instanceof UUID) return (UUID) idObj;
            } catch (Exception alsoIgnored) {
                // Si no tiene ninguno de los dos, simplemente retornamos null
            }
        }
        return null;
    }
}
