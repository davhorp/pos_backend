package com.school.app.utils;

import com.school.app.dto.requets.ProductResponse;
import com.school.app.dto.response.CashShiftResponse;
import com.school.app.dto.response.CloseCashShiftResponse;
import com.school.app.entity.CashShift;
import com.school.app.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;

@Component
public class UtilsPOS {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PREFIX = "TX-";

    public CashShiftResponse mapToResponse(CashShift shift) {
        return new CashShiftResponse(
                shift.getId(),
                shift.getStartTime(),            // CORREGIDO: En la entidad unificada es startTime
                shift.getEndTime(),              // CORREGIDO: En la entidad unificada es endTime
                shift.getStartingCash(),         // CORREGIDO: En la entidad unificada es startingCash
                shift.getDeclaredCash(),         // CORREGIDO: En la entidad unificada es declaredCash (lo que declaró el cajero)
                shift.getStatus().name(),
                shift.getUser().getUsername()    // CORREGIDO: En la entidad unificada la relación se llama 'user', no 'cashier'
        );
    }

    public CloseCashShiftResponse mapToResponseCloseShift(CashShift shift) {
        return new CloseCashShiftResponse(
                shift.getId(),
                shift.getStartTime(),            // CORREGIDO: En la entidad unificada es startTime
                shift.getEndTime(),              // CORREGIDO: En la entidad unificada es endTime
                shift.getStartingCash(),         // CORREGIDO: En la entidad unificada es startingCash
                shift.getDeclaredCash(),         // CORREGIDO: En la entidad unificada es declaredCash (lo que declaró el cajero)
                shift.getDiscrepancyCash(),
                shift.getStatus().name(),
                shift.getUser().getUsername(),    // CORREGIDO: En la entidad unificada la relación se llama 'user', no 'cashier'
                shift.getClosingTicketHtml()
        );
    }

    /**
     * Mapea una entidad Product a su DTO de respuesta ProductResponse.
     * Convierte los datos crudos de la base de datos en un formato ligero y
     * seguro para enviarse a Angular.
     *
     * @param product La entidad Product extraída de la base de datos.
     * @return El record ProductResponse.
     */
    public ProductResponse mapToProductResponse(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getBarcode(),
                product.getCurrentPrice(),
                product.getImageUrl()
        );
    }

    /**
     * Genera un ID completamente aleatorio de 20 posiciones.
     * Ejemplo de salida: "74839201847563920184"
     */
    public String generatePureNumericUUID() {
        StringBuilder sb = new StringBuilder(20);
        // El primer dígito debe ser del 1 al 9 para evitar que el número empiece con cero
        sb.append(SECURE_RANDOM.nextInt(9) + 1);
        // Generar los 19 dígitos restantes (del 0 al 9)
        for (int i = 0; i < 19; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Genera un identificador con el formato TX-XXXXXXXXX (9 dígitos).
     * Ejemplo: TX-004519283, TX-982103441
     */
    public static String generateTxNumber() {
        // Genera un número aleatorio entre 0 y 999,999,999
        int randomNumber = SECURE_RANDOM.nextInt(1_000_000_000);
        // %09d formatea el número para que SIEMPRE tenga 9 posiciones,
        // rellenando con ceros a la izquierda si el número generado es menor.
        return PREFIX + String.format("%09d", randomNumber);
    }

    /**
     * Utilidad interna para calcular la diferencia porcentual entre dos cifras.
     * Evita divisiones por cero devolviendo 100% si en el periodo anterior no hubo ventas.
     */
    public double calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        // Fórmula: ((Actual - Anterior) / Anterior) * 100
        BigDecimal difference = current.subtract(previous);
        BigDecimal percentage = difference.divide(previous, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        return percentage.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
