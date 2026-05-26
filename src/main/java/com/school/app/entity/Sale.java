package com.school.app.entity;

import com.school.app.utils.UtilsPOS;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sales")
@Getter
@Setter
//@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    private final UtilsPOS utilsPOS;

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
    @Column(nullable = false)
    private LocalDateTime saleDate;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // Relación bidireccional (Una venta tiene Muchos detalles)
    // CascadeType.ALL permite guardar la venta y sus detalles en una sola llamada a save()
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleItem> details = new ArrayList<>();

    // En tu archivo Sale.java agrega esto:
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_shift_id", nullable = false)
    private CashShift cashShift;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SaleTicket ticketDocument;

    @Column(name = "amount_tendered", precision = 12, scale = 2)
    private BigDecimal amountTendered; // Billete o moneda que entregó el cliente

    @Column(name = "change_amount", precision = 12, scale = 2)
    private BigDecimal changeAmount; // Lo que se le devolvió
    // Tipo de tarjeta (VISA, MASTERCARD, AMEX)
    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    // Últimos 4 dígitos para que el cliente identifique con qué tarjeta pagó
    @Column(name = "last_four_digits", length = 4)
    private String lastFourDigits;

    // Número de autorización o folio bancario (El dato MÁS importante para el contador)
    @Column(name = "auth_code", length = 50)
    private String authCode;
    @Column(name = "ticket_number", length = 20, updatable = false, nullable = false)
    private String ticketNumber;
    @Column(name = "transaction_id", length = 12, unique = true, updatable = false)
    private String transactionId;

    /**
     * Este método se ejecuta automáticamente justo antes de guardar
     * el registro en la base de datos por primera vez.
     */
    @PrePersist
    protected void onCreate() {
        if (this.transactionId == null) {
            this.transactionId = UtilsPOS.generateTxNumber();
        }

        // Aquí también suele ir tu inicialización de fecha:
        // if (this.createdAt == null) this.createdAt = OffsetDateTime.now();
    }

    // Método de conveniencia para mantener la sincronización bidireccional
    public void addDetail(SaleItem detail) {
        details.add(detail);
        detail.setSale(this);
    }

    public enum PaymentMethod {
        CASH, CREDIT_CARD, DEBIT_CARD, QR, TRANSFER
    }
}
