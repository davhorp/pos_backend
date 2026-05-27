package com.school.app.dto.response;

import java.math.BigDecimal;

public record WalletBalanceResponse(
        String phoneNumber,
        String ticketBalanceHtmlContent,
        BigDecimal balance
) {
}
