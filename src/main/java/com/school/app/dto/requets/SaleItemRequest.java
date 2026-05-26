package com.school.app.dto.requets;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemRequest(
        UUID productId,
        BigDecimal quantity,
        BigDecimal unitPrice
) {
}
