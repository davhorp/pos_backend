package com.school.app.services.ticket;

import com.school.app.audit.Auditable;
import com.school.app.dto.response.TicketResponse;
import com.school.app.entity.*;
import com.school.app.enums.WalletTxType;
import com.school.app.repository.SaleRepository;
import com.school.app.repository.SaleTicketRepository;
import com.school.app.repository.WalletTransactionRepository;
import com.school.app.services.auth.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio encargado de la generación y formato de tickets para impresoras térmicas POS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final SaleRepository saleRepository;
    private final SaleTicketRepository saleTicketRepository;
    private final AuditLogService auditLogService;
    private final WalletTransactionRepository walletTransactionRepository;
    // Ancho estándar para impresoras térmicas de 58mm
    private static final int TICKET_WIDTH = 47;

    /**
     * Genera el contenido de texto plano formateado para el ticket de venta.
     */
    @Auditable(action = SystemAuditLog.AuditAction.IMPRIMIR_TICKET, entityName = "SALE_TICKET")
    @Transactional
    public TicketResponse generateThermalTicket(UUID saleId) {
        long startTime = System.currentTimeMillis();
        boolean exito = true;
        String errorMsg = null;
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        log.info("Iniciando generación de ticket térmico para la venta con ID: {}", saleId);

        try {
            Sale sale = saleRepository.findById(saleId).orElseThrow(() -> {
                log.error("Fallo al generar ticket: No se encontró la venta con ID {}", saleId);
                return new IllegalArgumentException("Venta no encontrada con ID: " + saleId);
            });

            log.info("Venta encontrada. Cajero: {}, Artículos: {}", sale.getUser().getUsername(), sale.getItems().size());
            StringBuilder ticket = new StringBuilder();

            // 1. CABECERA DE LA TIENDA
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
            // 2. METADATOS DE LA VENTA
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            ticket.append("FECHA: ").append(sale.getSaleDate().format(formatter)).append("\n");
            ticket.append("TICKET: ").append(sale.getId().toString().substring(0, 8).toUpperCase()).append("\n");
            ticket.append("CAJERO: ").append(sale.getUser().getUsername()).append("\n");
            ticket.append("TURNO: ").append(sale.getCashShift().getId().toString().substring(0, 8).toUpperCase()).append("\n");
            ticket.append(divider()).append("\n");

            // 3. ENCABEZADOS DE PRODUCTOS
            ticket.append(leftRightText("CANT  DESCRIPCION", "IMPORTE")).append("\n");
            ticket.append(divider()).append("\n");

            // 4. DETALLE DE PRODUCTOS
            log.info("Procesando líneas de productos...");
            for (SaleItem item : sale.getItems()) {
                // Llamamos al nuevo método auxiliar
                String productLine = formatProductLine(item);
                // Formateamos el subtotal
                String subtotalStr = String.format("$%.2f", item.getSubtotal());
                // Ensamblamos la línea en el ticket
                ticket.append(leftRightText(productLine, subtotalStr)).append("\n");
            }
            ticket.append("\n");
            ticket.append(leftRightText("Articulos vendidos: " + sale.getItems().size(), "Transacción: " + sale.getTransactionId())).append("\n");
            ticket.append(divider()).append("\n");
            // 5. TOTALES Y PAGOS (Reemplazo por un Switch más limpio y tipado)
            log.info("Procesando sección de pagos (Método: {})...", sale.getPaymentMethod());
            BigDecimal walletRedeemed = sale.getWalletRedeemed() != null ? sale.getWalletRedeemed() : BigDecimal.ZERO;
            if (walletRedeemed.compareTo(BigDecimal.ZERO) > 0) {
                ticket.append(leftRightText("SUBTOTAL:", String.format("$%.2f", sale.getTotalAmount()))).append("\n");
                ticket.append(leftRightText("DESC. MONEDERO:", String.format("-$%.2f", walletRedeemed))).append("\n");
                BigDecimal totalReal = sale.getTotalAmount().subtract(walletRedeemed);
                ticket.append(leftRightText("TOTAL A PAGAR:", String.format("$%.2f", totalReal))).append("\n");
            } else {
                ticket.append(leftRightText("TOTAL A PAGAR:", String.format("$%.2f", sale.getTotalAmount()))).append("\n");
            }
            switch (sale.getPaymentMethod()) {
                case CASH:
                    String recibido = sale.getAmountTendered() != null ? String.format("$%.2f", sale.getAmountTendered()) : "$0.00";
                    String cambio = sale.getChangeAmount() != null ? String.format("$%.2f", sale.getChangeAmount()) : "$0.00";
                    ticket.append(leftRightText("EFECTIVO RECIBIDO:", recibido)).append("\n");
                    ticket.append(leftRightText("SU CAMBIO:", cambio)).append("\n");
                    break;
                case CREDIT_CARD:
                case DEBIT_CARD:
                    String brand = sale.getCardBrand() != null ? sale.getCardBrand() : "TARJETA";
                    String last4 = sale.getLastFourDigits() != null ? "****" + sale.getLastFourDigits() : "";
                    String auth = sale.getAuthCode() != null ? sale.getAuthCode() : "APROBADO";
                    ticket.append(leftRightText("PAGADO CON:", brand + " " + last4)).append("\n");
                    ticket.append(leftRightText("AUTORIZACION:", auth)).append("\n");
                    ticket.append("\n");
                    ticket.append("\n").append(centerText("PAGO EN UNA SOLA EXHIBICION")).append("\n");
                    break;
                case TRANSFER:
                    String banco = sale.getCardBrand() != null ? sale.getCardBrand() : "SPEI";
                    String rastreo = sale.getAuthCode() != null ? sale.getAuthCode() : "N/A";
                    ticket.append(leftRightText("PAGADO CON:", "TRANSFERENCIA " + banco)).append("\n");
                    ticket.append(leftRightText("CLAVE RASTREO:", rastreo)).append("\n");
                    ticket.append("\n");
                    ticket.append("\n").append(centerText("PAGO RECIBIDO VIA SPEI")).append("\n");
                    break;
                case QR:
                    String plataforma = sale.getCardBrand() != null ? sale.getCardBrand() : "APP DIGITAL";
                    String operacion = sale.getAuthCode() != null ? sale.getAuthCode() : "N/A";
                    ticket.append(leftRightText("PAGADO CON:", "CODIGO QR " + plataforma)).append("\n");
                    ticket.append(leftRightText("OPERACION:", operacion)).append("\n");
                    ticket.append("\n");
                    ticket.append("\n").append(centerText("PAGO POR MEDIOS DIGITALES")).append("\n");
                    break;
            }
            // 6. SECCIÓN DE RECOMPENSAS / MONEDERO DIGITAL 🔥
            // Buscamos si en ESTA venta el cliente acumuló puntos usando el TransactionId
            Optional<WalletTransaction> earnedTx = walletTransactionRepository
                    .findByReferenceTicketAndTransactionType(sale.getTransactionId(), WalletTxType.ACCUMULATION);

            if (earnedTx.isPresent() || walletRedeemed.compareTo(BigDecimal.ZERO) > 0) {
                ticket.append(divider()).append("\n");
                ticket.append(centerText("--- MONEDERO DIGITAL DAVHO'S ---")).append("\n");

                if (walletRedeemed.compareTo(BigDecimal.ZERO) > 0) {
                    ticket.append(leftRightText("Saldo Utilizado:", String.format("-$%.2f", walletRedeemed))).append("\n");
                }

                if (earnedTx.isPresent()) {
                    ticket.append(leftRightText("Puntos Ganados:", String.format("+$%.2f", earnedTx.get().getAmount()))).append("\n");
                    ticket.append(leftRightText("Saldo Disponible:", String.format("$%.2f", earnedTx.get().getWallet().getBalance()))).append("\n");
                }
            }
            ticket.append(asterisk()).append("\n");
            ticket.append(centerText("Recuerda que puedes realizar")).append("\n");
            ticket.append(centerText("recargas de tiempo aire en todas")).append("\n");
            ticket.append(centerText("nuestras sucursales sin comision")).append("\n");
            ticket.append(asterisk()).append("\n");
            // 6. PIE DE PÁGINA
            ticket.append(divider()).append("\n");
            ticket.append(centerText("¡Gracias por su compra!")).append("\n");
            ticket.append(centerText("Este ticket no es un")).append("\n");
            ticket.append(centerText("comprobante fiscal.")).append("\n");
            ticket.append(divider()).append("\n");
            log.info("Contenido del ticket ensamblado. Guardando en base de datos...");
            SaleTicket document = SaleTicket.builder()
                    .sale(sale)
                    .ticketContent(ticket.toString())
                    .build();
            SaleTicket savedTicket = saleTicketRepository.save(document);
            log.info(ticket.toString());
            log.info("Ticket guardado exitosamente con ID: {}. Proceso finalizado.", savedTicket.getId());
            return new TicketResponse(ticket.toString(), sale.getTicketNumber());
        } catch (Exception e) {
            exito = false;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            auditLogService.logActivity(
                    "IMPRIMIR_TICKET",
                    "TicketService.generateThermalTicket",
                    "Generación de ticket para saleId: " + saleId,
                    startTime,
                    exito,
                    errorMsg,
                    request.getRemoteAddr(),
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getUserPrincipal().getName());
        }
    }

    /**
     * Formatea la línea de descripción del producto para el ticket.
     * Convierte la cantidad a piezas o kilos según corresponda y trunca a 33 caracteres.
     */
    private String formatProductLine(SaleItem item) {
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

    // --- MÉTODOS AUXILIARES PARA FORMATO DE TEXTO ---

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
