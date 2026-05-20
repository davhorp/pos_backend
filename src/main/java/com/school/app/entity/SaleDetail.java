package com.school.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sale_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // La referencia de vuelta hacia la cabecera de la venta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    // Relación con el producto vendido
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "historical_unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal historicalUnitPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}
