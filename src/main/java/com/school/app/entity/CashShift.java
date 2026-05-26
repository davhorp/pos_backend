package com.school.app.entity;

import com.school.app.enums.ShiftStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cash_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashShift {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // --- RELACIÓN CON EL USUARIO (CAJERO) ---
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // --- ESTADO ---
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShiftStatus status = ShiftStatus.OPEN;

    // --- TIEMPOS ---
    @Column(name = "start_time", updatable = false) // Le quitamos el nullable = false
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    // --- ARQUEO PRINCIPAL (Usando BigDecimal para precisión financiera) ---
    @Column(name = "starting_cash", precision = 12, scale = 2)
    private BigDecimal startingCash; // Fondo de caja con el que abrió

    @Column(name = "expected_cash", precision = 12, scale = 2)
    private BigDecimal expectedCash; // Lo que debería haber (Fondo + Ventas Efectivo - Retiros)

    @Column(name = "declared_cash", precision = 12, scale = 2)
    private BigDecimal declaredCash; // Lo que el cajero contó y capturó al cerrar

    @Column(name = "discrepancy_cash", precision = 12, scale = 2)
    private BigDecimal discrepancyCash; // Faltante (-) o Sobrante (+)
    @Column(name = "discrepancy_reason", columnDefinition = "TEXT")
    private String discrepancyReason;
    // --- DESGLOSE DE VENTAS POR MÉTODO DE PAGO ---
    @Builder.Default
    @Column(name = "cash_payouts", precision = 12, scale = 2, nullable = false)
    private BigDecimal cashPayouts = BigDecimal.ZERO; // Retiros de dinero para pagos a proveedores, etc.
    @Column(name = "total_sales", precision = 12, scale = 2)
    private BigDecimal totalSales; // Suma absoluta de todas las ventas (Efectivo + Tarjeta + QR + SPEI)
    @Builder.Default
    @Column(name = "cash_sales", precision = 12, scale = 2, nullable = false)
    private BigDecimal cashSales = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "card_sales", precision = 12, scale = 2, nullable = false)
    private BigDecimal cardSales = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "transfer_sales", precision = 12, scale = 2, nullable = false)
    private BigDecimal transferSales = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "qr_sales", precision = 12, scale = 2, nullable = false)
    private BigDecimal qrSales = BigDecimal.ZERO;

    @Column(length = 255)
    private String notes;

    // --- RELACIÓN BIDIRECCIONAL CON VENTAS ---
    @OneToMany(mappedBy = "cashShift", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Sale> sales = new ArrayList<>();

    // --- AUDITORÍA DE BASE DE DATOS ---
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // Guardaremos el HTML/Texto del ticket generado para poder reimprimirlo
    @Column(name = "closing_ticket_html", columnDefinition = "TEXT")
    private String closingTicketHtml;

    // --- MÉTODOS DEL CICLO DE VIDA (JPA) ---
    @PrePersist
    protected void onCreate() {
        if (this.startTime == null) {
            this.startTime = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ShiftStatus.OPEN;
        }
    }

    // --- MÉTODO DE CONVENIENCIA ---
    public void addSale(Sale sale) {
        sales.add(sale);
        sale.setCashShift(this);
    }

}
