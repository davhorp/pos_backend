package com.school.app.dto.requets;

import java.math.BigDecimal;

public record CloseShiftRequest(
        BigDecimal declaredCash,
        String discrepancyReason
) {
}
