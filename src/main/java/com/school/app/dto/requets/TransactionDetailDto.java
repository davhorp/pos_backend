package com.school.app.dto.requets;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public record TransactionDetailDto(
        OffsetDateTime date,
        String type,
        BigDecimal amount,
        String referenceTicket,
        List<ProductSummaryDto> products
) {
}
