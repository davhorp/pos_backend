package com.school.app.dto.response;

import java.math.BigDecimal;

public record WalletCheckResponse(
        BigDecimal balance
) {
}
