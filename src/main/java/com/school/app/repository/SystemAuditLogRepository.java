package com.school.app.repository;

import com.school.app.entity.SystemAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, UUID> {

    // Buscar todo lo que hizo un cajero en específico
    List<SystemAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Buscar eventos por tipo (ej. ver todas las aperturas de caja)
    List<SystemAuditLog> findByActionTypeOrderByCreatedAtDesc(SystemAuditLog.AuditAction actionType);

    // Buscar qué pasó con un registro en específico (ej. el historial de un Producto o Venta)
    List<SystemAuditLog> findByEntityNameAndEntityIdOrderByCreatedAtDesc(String entityName, UUID entityId);

    // Buscar eventos en un rango de fechas (ej. auditoría del día de hoy)
    List<SystemAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(OffsetDateTime start, OffsetDateTime end);
}
