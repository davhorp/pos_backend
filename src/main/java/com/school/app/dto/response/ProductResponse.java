package com.school.app.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String barcode,
        String name,
        BigDecimal price,       // Mapeado desde currentPrice
        BigDecimal stockQuantity,
        String category
) {
}
