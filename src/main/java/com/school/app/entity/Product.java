package com.school.app.entity;

import com.school.app.enums.ProductCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String barcode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "current_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "stock_quantity", nullable = false, precision = 10, scale = 3)
    private BigDecimal stockQuantity;

    @Column(name = "min_stock", nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal minStock = new BigDecimal("5.000");

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "unit_of_measure", length = 20)
    private String unitOfMeasure = "PZA"; // Valor por defecto seguro

    // 🔥 CAMBIO: Nueva columna de categoría
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ProductCategory category = ProductCategory.ABARROTES;
}
