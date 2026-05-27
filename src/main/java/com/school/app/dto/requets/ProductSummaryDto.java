package com.school.app.dto.requets;

import java.math.BigDecimal;

public record ProductSummaryDto(
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice
) {
}
