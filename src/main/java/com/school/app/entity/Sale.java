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
@NoArgsConstructor // Requerido por JPA
@AllArgsConstructor
@Builder
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_shift_id", nullable = false)
    private CashShift cashShift;

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

    // Solo una lista para los ítems
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SaleTicket ticketDocument;

    // Campos de efectivo
    @Column(name = "amount_tendered", precision = 12, scale = 2)
    private BigDecimal amountTendered;

    @Column(name = "change_amount", precision = 12, scale = 2)
    private BigDecimal changeAmount;

    // Campos Tarjeta
    @Column(name = "card_brand", length = 20)
    private String cardBrand;
    @Column(name = "last_four_digits", length = 4)
    private String lastFourDigits;
    @Column(name = "auth_code", length = 50)
    private String authCode;

    // Campos Transferencia / QR
    @Column(name = "bank_name", length = 50)
    private String bankName;
    @Column(name = "tracking_key", length = 100)
    private String trackingKey;

    @Column(name = "ticket_number", length = 20, updatable = false, nullable = false)
    private String ticketNumber;

    @Column(name = "transaction_id", length = 12, unique = true, updatable = false)
    private String transactionId;

    @Column(name = "wallet_redeemed", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal walletRedeemed = BigDecimal.ZERO;

    @PrePersist
    protected void onCreate() {
        if (this.transactionId == null) {
            // Llama a la utilidad estática directamente
            this.transactionId = UtilsPOS.generateTxNumber();
        }
    }

    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
    }

    public enum PaymentMethod {
        CASH, CREDIT_CARD, DEBIT_CARD, QR, TRANSFER, ELECTRONIC_WALLET
    }
}
