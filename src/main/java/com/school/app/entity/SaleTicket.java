package com.school.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad que representa la "fotografía" física o el documento estático
 * del ticket generado para una venta en un momento específico.
 */
@Entity
@Table(name = "sale_tickets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relación 1 a 1 con la Venta.
    // Una venta tiene un diseño de ticket original generado.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false, unique = true)
    private Sale sale;

    // Usamos columnDefinition = "TEXT" porque el ticket tendrá saltos de línea (\n)
    // y fácilmente superará los 255 caracteres por defecto de VARCHAR.
    @Column(name = "ticket_content", columnDefinition = "TEXT", nullable = false)
    private String ticketContent;

    // Fecha exacta en la que se generó y guardó esta copia del ticket
    @Column(name = "generated_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime generatedAt;
}
