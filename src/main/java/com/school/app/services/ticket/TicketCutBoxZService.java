package com.school.app.services.ticket;

import com.school.app.audit.Auditable;
import com.school.app.entity.CashShift;
import com.school.app.entity.SystemAuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCutBoxZService {

    // Ancho estándar para impresoras térmicas de 58mm
    private static final int TICKET_WIDTH = 41;
    private final SpringTemplateEngine templateEngine;

    /**
     * Genera el Reporte Z (Corte de Caja) en formato HTML para impresión térmica.
     * * @param shiftId El ID del turno a procesar.
     * @return El contenido HTML formateado como String.
     */
    @Auditable(action = SystemAuditLog.AuditAction.IMPRIMIR_TICKET_CORTE_Z, entityName = "SALE_TICKET")
    @Transactional
    public String generateZReportHtml(CashShift shift) {
        log.info("Iniciando generación de Corte Z (Reporte Z) en HTML para el turno con ID: {}", shift.getId());
        try {
            // 2. Preparar el contexto con las variables que Thymeleaf necesita
            Context context = new Context();
            context.setVariable("shift", shift);
            context.setVariable("shiftIdShort", shift.getId().toString().substring(0, 8).toUpperCase());
            // Nota: Thymeleaf se encargará de realizar las validaciones de nulos
            // y los formatos de moneda internamente en la plantilla.
            // 3. Procesar la plantilla HTML (Debe coincidir con el nombre del archivo en src/main/resources/templates/)
            String htmlContent = templateEngine.process("reporte-z", context);
            log.info("Reporte Z en HTML generado exitosamente para el turno: {}", shift.getId());
            // Retornamos el HTML final
            return htmlContent;
        } catch (Exception e) {
            log.error("Ocurrió un error inesperado al generar el Reporte Z para el turno {}: {}", shift.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Genera el formato de texto plano para el ticket de Corte Z,
     * optimizado para impresoras térmicas de 58mm (32 caracteres).
     */
    public String generateZReportText(CashShift shift) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        // 1. Extraer variables seguras

        String tipoDescuadre = shift.getDiscrepancyCash().compareTo(BigDecimal.ZERO) >= 0 ? "SOBRANTE" : "FALTANTE";
        String discrepanciaAbs = String.format("$%.2f", shift.getDiscrepancyCash().abs());

        // 2. Construir el Ticket
        StringBuilder ticket = new StringBuilder();

        log.info("Construyendo cabecera de la sucursal...");
        ticket.append("\n");
        ticket.append(divider()).append("\n");
        ticket.append(centerText("DAVHO´s S.A. DE C.V.")).append("\n");
        ticket.append(centerText("Av. Pdte, Masaryk 8,")).append("\n");
        ticket.append(centerText("Polanco V Secc, Miguel Hidalgo")).append("\n");
        ticket.append(centerText("C.P. 11560, CMDX")).append("\n");
        ticket.append(centerText("RFC: REPJ545667RD7")).append("\n");
        ticket.append(centerText("Personal Moral Regimen General de Ley")).append("\n");
        ticket.append(divider()).append("\n");
        // 1.2 SUCURSAL
        ticket.append(centerText("Sucursal: 385 - La Curva")).append("\n");
        ticket.append(centerText("Av. Morelos sn lt, Fracc B")).append("\n");
        ticket.append(centerText("Col. Valle de Ecatepec, Estado Mexico")).append("\n");
        ticket.append(divider()).append("\n");
        // METADATOS
        ticket.append("FECHA: ").append(LocalDateTime.now().toLocalDate().format(formatter)).append("\n");
        ticket.append("TURNO:  ").append(shift.getId().toString().substring(0, 8).toUpperCase()).append("\n");
        ticket.append("CAJERO: ").append(shift.getUser().getFullName() != null ? shift.getUser().getFullName() : "CAJERO").append("\n");
        ticket.append("ABRE:   ").append(shift.getStartTime() != null ? shift.getStartTime().format(formatter) : "N/A").append("\n");
        ticket.append("CIERRA: ").append(shift.getEndTime() != null ? shift.getEndTime().format(formatter) : "N/A").append("\n");
        ticket.append(divider()).append("\n");
        // INGRESOS
        ticket.append(centerText("INGRESOS POR METODO")).append("\n");
        ticket.append(divider()).append("\n");
        ticket.append(leftRightText("EFECTIVO:", String.format("$%.2f", shift.getCashSales()))).append("\n");
        ticket.append(leftRightText("TARJETA:", String.format("$%.2f", shift.getCardSales()))).append("\n");
        ticket.append(leftRightText("TRANSFERENCIA:", String.format("$%.2f", shift.getTransferSales()))).append("\n");
        ticket.append(leftRightText("PAGO QR:", String.format("$%.2f", shift.getQrSales()))).append("\n");
        ticket.append(divider()).append("\n");
        ticket.append(leftRightText("TOTAL VENTAS:", String.format("$%.2f", shift.getTotalSales()))).append("\n");
        ticket.append(divider()).append("\n");
        // Requiere que agregues estos campos a tu entidad CashShift
        BigDecimal walletRedeemed = shift.getWalletRedeemed() != null ? shift.getWalletRedeemed() : BigDecimal.ZERO;
        BigDecimal walletAwarded = shift.getWalletAwarded() != null ? shift.getWalletAwarded() : BigDecimal.ZERO;
        ticket.append(centerText("MOVIMIENTOS MONEDERO")).append("\n");
        ticket.append(divider()).append("\n");
        ticket.append(leftRightText("SALDO CANJEADO:", String.format("-$%.2f", walletRedeemed))).append("\n");
        ticket.append(leftRightText("PUNTOS OTORGADOS:", String.format("+$%.2f", walletAwarded))).append("\n");
        ticket.append(divider()).append("\n");
        // CUADRE FÍSICO
        ticket.append(centerText("CUADRE DE EFECTIVO")).append("\n");
        ticket.append(divider()).append("\n");
        ticket.append(leftRightText("FONDO INICIAL:", String.format("$%.2f", shift.getStartingCash()))).append("\n");
        ticket.append(leftRightText("+ V. EFECTIVO:", String.format("$%.2f", shift.getCashSales()))).append("\n");
        ticket.append(leftRightText("= ESPERADO:", String.format("$%.2f", shift.getExpectedCash()))).append("\n");
        ticket.append(leftRightText("DECLARADO:", String.format("$%.2f", shift.getDeclaredCash()))).append("\n");
        ticket.append(divider()).append("\n");
        ticket.append(leftRightText(tipoDescuadre + ":", discrepanciaAbs)).append("\n");
        ticket.append(divider()).append("\n");
        // PIE DE PÁGINA Y FIRMAS
        ticket.append("\n\n\n");
        ticket.append(centerText("_________________________")).append("\n");
        ticket.append(centerText("FIRMA DEL CAJERO")).append("\n");
        ticket.append("\n");
        ticket.append(centerText("Software por EduCore")).append("\n");
        ticket.append(centerText("Ari Capital Humano")).append("\n");
        ticket.append(divider()).append("\n\n"); // Doble salto final para que la guillotina corte bien

        return ticket.toString();
    }

    private String centerText(String text) {
        if (text.length() >= TICKET_WIDTH) return text.substring(0, TICKET_WIDTH);
        int spaces = (TICKET_WIDTH - text.length()) / 2;
        return " ".repeat(spaces) + text + " ".repeat(TICKET_WIDTH - text.length() - spaces);
    }

    private String leftRightText(String left, String right) {
        if (left.length() + right.length() >= TICKET_WIDTH) {
            return left.substring(0, TICKET_WIDTH - right.length() - 1) + " " + right;
        }
        int spaces = TICKET_WIDTH - left.length() - right.length();
        return left + " ".repeat(spaces) + right;
    }

    private String divider() {
        return "-".repeat(TICKET_WIDTH);
    }

    private String asterisk() {
        return "*".repeat(TICKET_WIDTH);
    }

}
