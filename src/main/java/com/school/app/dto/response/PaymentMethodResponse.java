package com.school.app.dto.response;

import java.math.BigDecimal;

/**
 * DTO que representa el desglose de ingresos agrupados por método de pago.
 * Contiene el color de la marca para renderizado dinámico en el Frontend.
 */
public record PaymentMethodResponse(
        String name,
        BigDecimal amount,
        String color
) {
}
