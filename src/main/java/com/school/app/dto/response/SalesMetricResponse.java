package com.school.app.dto.response;

import java.math.BigDecimal;

public record SalesMetricResponse(
        String title,
        BigDecimal value,
        double percentageChange,
        String icon,
        boolean isCurrency
) {
}
