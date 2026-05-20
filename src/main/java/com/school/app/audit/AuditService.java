package com.school.app.audit;

import com.school.app.entity.AuditLog;
import com.school.app.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async("auditExecutor") // Se recomienda configurar un Executor personalizado
    public void saveLog(String accion, String target, String modulo, String detalles,
                        String ip, String method, String uri, long tiempo,
                        boolean exito, String error) {
        AuditLog auditLog = AuditLog.builder()
                .accion(accion)
                .usuario(target) // En tu caso, nombreUsuario es el target
                .modulo(modulo)
                .detalles("Argumentos: " + detalles)
                .ipAddress(ip)
                .httpMethod(method)
                .endpoint(uri)
                .tiempoEjecucion(tiempo)
                .exito(exito)
                .mensajeError(error)
                .fecha(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }
}
