package com.school.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Puede ser null si el evento es un intento de login fallido de un usuario inexistente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private AuditAction actionType;

    // Qué entidad fue afectada (ej. "PRODUCT", "SALE", "REPORT")
    @Column(name = "entity_name", length = 50)
    private String entityName;

    // El ID del registro afectado (ej. el UUID del producto o venta modificado)
    @Column(name = "entity_id")
    private UUID entityId;

    // Dirección IP desde donde se realizó la acción (útil para detectar fraudes)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // JSONB en PostgreSQL: Ideal para guardar un snapshot de cómo quedó el dato o filtros del reporte
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String detailsPayload;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // Catálogo de acciones auditables
    public enum AuditAction {
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        REGISTRO_VENTA_SISTEMA,
        ABONO_MONEDERO,
        TICKET_MONEDERO,
        PRODUCTOS_ACTIVOS,
        CONSULTAR_DASHBOARD,
        VIEW_ADMIN_DASHBOARD,
        DEDUCIR_SALDO_WALLET,
        SEED_USER,
        MONEDERO_TRANSACCION,
        USER_CREATED,
        BUSCAR_PRODUCTO,
        SEARCH_PRODUCT,
        ALL_PRODUCTS,
        PROCESAR_VENTA,
        OPEN_SHIFT,
        CERRAR_CAJA,
        CONSULTAR_MONEDERO,
        VERIFICAR_TURNO_ACTIVO,
        IMPRIMIR_TICKET,
        CLOSE_SHIFT,
        ABRIR_CAJA,
        LOGOUT,
        CASH_REGISTER_OPENED,
        CASH_REGISTER_CLOSED,
        SALE_COMPLETED,
        SALE_VOIDED,          // Cancelación de venta (requeriría permiso ADMIN)
        REPORT_GENERATED,
        INVENTORY_ADJUSTED,   // Si un ADMIN modifica el stock manualmente
        PRODUCT_CREATED
    }
}
