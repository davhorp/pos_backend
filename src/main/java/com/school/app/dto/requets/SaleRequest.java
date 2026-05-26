package com.school.app.dto.requets;

import java.math.BigDecimal;
import java.util.List;

public record SaleRequest(
        String paymentMethod,
        BigDecimal totalAmount,
        BigDecimal amountTendered,
        List<SaleItemRequest> items,
        String cardBrand,
        String lastFourDigits,
        String authCode,
        String customerPhone,
        String bankName,
        BigDecimal walletRedeemedAmount,
        String trackingKey
) {
}
