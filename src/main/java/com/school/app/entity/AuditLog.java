package com.school.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String accion;       // Ejemplo: "CREAR_USUARIO"
    private String usuario;      // Quién lo hizo
    private String modulo;       // En qué servicio/controlador
    private String detalles;     // Parámetros de entrada o JSON
    private String ipAddress;    // Desde dónde
    private LocalDateTime fecha; // Cuándo
    private String httpMethod;   // GET, POST, etc.
    private String endpoint;     // /api/v1/resource
    private String mensajeError;     // /api/v1/resource
    private Long tiempoEjecucion; // milisegundos
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean exito; // JSON de parámetros o mensaje de error

}
