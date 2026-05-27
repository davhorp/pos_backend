package com.school.app.utils;

import com.school.app.dto.response.CashShiftResponse;
import com.school.app.dto.response.CloseCashShiftResponse;
import com.school.app.dto.response.ProductResponse;
import com.school.app.dto.response.SearchProductResponse;
import com.school.app.entity.CashShift;
import com.school.app.entity.Product;
import com.school.app.entity.SaleItem;
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

    /**
     * Mapea una entidad Product a un record ProductResponse.
     */
    public ProductResponse mapToResponseProduct(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getBarcode(),
                product.getName(),
                product.getCurrentPrice(),
                product.getStockQuantity().intValue(),
                product.getUnitOfMeasure(),
                product.getImageUrl(),
                product.getCategory().name() // Convierte el Enum a String
        );
    }

    /**
     * Formatea la línea de descripción del producto para el ticket.
     * Convierte la cantidad a piezas o kilos según corresponda y trunca a 33 caracteres.
     */
    public String formatProductLine(SaleItem item) {
        BigDecimal qty = item.getQuantity();
        String productName = item.getProduct().getName();
        String productLine;
        // Evaluar si es un número entero (piezas) o fraccionario (granel)
        if (qty.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            int piezas = qty.intValue();
            String sufijo = (piezas == 1) ? "pz" : "pzas";
            productLine = String.format("%d%s %s", piezas, sufijo, productName);
        } else {
            // Limpiar ceros inútiles del peso
            String pesoLimpio = qty.stripTrailingZeros().toPlainString();
            productLine = String.format("%sKg %s", pesoLimpio, productName);
        }
        // Truncar a 33 caracteres para respetar el margen de la impresora térmica
        if (productLine.length() > 33) {
            return productLine.substring(0, 33);
        }
        return productLine;
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
    public SearchProductResponse mapToProductResponse(Product product) {
        if (product == null) {
            return null;
        }
        return new SearchProductResponse(
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

    public String centerText(String text, int ticketWidth) {
        if (text.length() >= ticketWidth) return text.substring(0, ticketWidth);
        int spaces = (ticketWidth - text.length()) / 2;
        return " ".repeat(spaces) + text + " ".repeat(ticketWidth - text.length() - spaces);
    }

    public String leftRightText(String left, String right, int ticketWidth) {
        if (left.length() + right.length() >= ticketWidth) {
            return left.substring(0, ticketWidth - right.length() - 1) + " " + right;
        }
        int spaces = ticketWidth - left.length() - right.length();
        return left + " ".repeat(spaces) + right;
    }

    public String divider(int ticketWidth) {
        return "-".repeat(ticketWidth);
    }

    public String asterisk(int ticketWidth) {
        return "*".repeat(ticketWidth);
    }
}
