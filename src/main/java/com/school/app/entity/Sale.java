package com.school.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relación con el vendedor (Muchos a Uno: Muchas ventas las hace Un usuario)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // Relación bidireccional (Una venta tiene Muchos detalles)
    // CascadeType.ALL permite guardar la venta y sus detalles en una sola llamada a save()
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleDetail> details = new ArrayList<>();

    // NUEVO: Vincular la venta al turno activo del cajero
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private CashRegisterShift shift;

    // Método de conveniencia para mantener la sincronización bidireccional
    public void addDetail(SaleDetail detail) {
        details.add(detail);
        detail.setSale(this);
    }

    public enum PaymentMethod {
        CASH, CREDIT_CARD, DEBIT_CARD, QR, TRANSFER
    }
}
