package com.school.app.dto.requets;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String barcode,
        BigDecimal price,
        String imageUrl
) {
}
