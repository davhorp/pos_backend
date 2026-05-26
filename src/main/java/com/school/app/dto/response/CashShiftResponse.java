package com.school.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CashShiftResponse(
        UUID id,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        String status,
        String username
) {
}
