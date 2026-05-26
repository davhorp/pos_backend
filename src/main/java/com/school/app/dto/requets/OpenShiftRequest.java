package com.school.app.dto.requets;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OpenShiftRequest(
        @NotNull(message = "El fondo inicial es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El fondo inicial no puede ser negativo")
        BigDecimal openingBalance
) {
}
