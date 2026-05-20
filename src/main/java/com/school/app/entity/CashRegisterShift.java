package com.school.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cash_register_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegisterShift {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // El cajero que abrió este turno
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "opened_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    // El fondo de caja inicial (ej. $500 pesos en morralla)
    @Column(name = "opening_balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal openingBalance;

    // Cuánto dinero real reporta el cajero al cerrar (para detectar faltantes/sobrantes)
    @Column(name = "closing_balance", precision = 10, scale = 2)
    private BigDecimal closingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShiftStatus status = ShiftStatus.OPEN;

    public enum ShiftStatus {
        OPEN, CLOSED
    }
}
