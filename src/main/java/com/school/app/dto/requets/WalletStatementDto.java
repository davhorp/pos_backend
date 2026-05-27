package com.school.app.dto.requets;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record WalletStatementDto(
        String phoneNumber,
        String customerName,
        BigDecimal currentBalance,
        LocalDateTime generationDate,
        List<TransactionDetailDto> transactions
) {
}
