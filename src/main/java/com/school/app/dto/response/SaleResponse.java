package com.school.app.dto.response;

import java.util.UUID;

public record SaleResponse(
        UUID ticketId,
        String message
) {
}
