package com.school.app.services.auth;

import com.school.app.entity.AuditLog;
import com.school.app.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Guarda un registro de auditoría.
     * @Async hace que se ejecute en un hilo separado para no bloquear la respuesta al usuario.
     */
    @Async
    public void logActivity(String accion, String modulo, String detalles, long startTime, boolean exito, String errorMsg, String ip, String httpMethod, String url, String user) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAccion(accion);
            auditLog.setModulo(modulo);
            auditLog.setDetalles(detalles);
            auditLog.setFecha(LocalDateTime.now());
            auditLog.setExito(exito);
            auditLog.setMensajeError(errorMsg);
            auditLog.setTiempoEjecucion(System.currentTimeMillis() - startTime);
            // Obtener datos dinámicos de la petición Web
                auditLog.setIpAddress(ip);
                auditLog.setHttpMethod(httpMethod);
                auditLog.setEndpoint(url);
                auditLog.setUsuario(user);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Loguear el error real para saber por qué falló la auditoría
            log.error("Ocurrió un error al guardar auditoría [Acción: {}]: {}", accion, e.getMessage());
        }
    }
}
