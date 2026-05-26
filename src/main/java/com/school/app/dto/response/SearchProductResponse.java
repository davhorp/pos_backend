package com.school.app.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record SearchProductResponse(
        UUID id,
        String name,
        String barcode,
        BigDecimal price,
        String imageUrl
) {
}
