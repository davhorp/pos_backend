package com.school.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CloseCashShiftResponse(
        UUID id,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        BigDecimal discrepancy,
        String status,
        String username,
        String corteZ
) {
}
