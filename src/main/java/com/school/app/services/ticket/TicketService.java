package com.school.app.services.ticket;

import com.school.app.audit.Auditable;
import com.school.app.dto.response.TicketResponse;
import com.school.app.entity.*;
import com.school.app.enums.WalletTxType;
import com.school.app.repository.SaleRepository;
import com.school.app.repository.SaleTicketRepository;
import com.school.app.repository.WalletTransactionRepository;
import com.school.app.services.auth.AuditLogService;
import com.school.app.utils.UtilsPOS;
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
    private final UtilsPOS utilsPOS;
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
            ticket.append(utilsPOS.divider(TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("DAVHO´s S.A. DE C.V.", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("Av. Pdte, Masaryk 8,", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("Polanco V Secc, Miguel Hidalgo", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("C.P. 11560, CMDX", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("RFC: REPJ545667RD7", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("Personal Moral Regimen General de Ley", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.divider(TICKET_WIDTH)).append("\n");
            // 1.2 SUCURSAL
            ticket.append(utilsPOS.centerText("Sucursal: 385 - La Curva", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("Av. Morelos sn lt, Fracc B", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("Col. Valle de Ecatepec, Estado Mexico", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.divider(TICKET_WIDTH)).append("\n");
            // 2. METADATOS DE LA VENTA
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            ticket.append("FECHA: ").append(sale.getSaleDate().format(formatter)).append("\n");
            ticket.append("TICKET: ").append(sale.getId().toString().substring(0, 8).toUpperCase()).append("\n");
            ticket.append("CAJERO: ").append(sale.getUser().getUsername()).append("\n");
            ticket.append("TURNO: ").append(sale.getCashShift().getId().toString().substring(0, 8).toUpperCase()).append("\n");
            ticket.append(utilsPOS.divider(TICKET_WIDTH)).append("\n");
            // 3. ENCABEZADOS DE PRODUCTOS
            ticket.append(utilsPOS.leftRightText("CANT  DESCRIPCION", "IMPORTE", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.divider(TICKET_WIDTH)).append("\n");
            // 4. DETALLE DE PRODUCTOS
            log.info("Procesando líneas de productos...");
            for (SaleItem item : sale.getItems()) {
                // Llamamos al nuevo método auxiliar
                String productLine = utilsPOS.formatProductLine(item);
                // Formateamos el subtotal
                String subtotalStr = String.format("$%.2f", item.getSubtotal());
                // Ensamblamos la línea en el ticket
                ticket.append(utilsPOS.leftRightText(productLine, subtotalStr, TICKET_WIDTH)).append("\n");
            }
            ticket.append("\n");
            ticket.append(utilsPOS.leftRightText("Articulos vendidos: " + sale.getItems().size(), "Transacción: " + sale.getTransactionId(), TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.divider(TICKET_WIDTH)).append("\n");
            // 5. TOTALES Y PAGOS (Reemplazo por un Switch más limpio y tipado)
            log.info("Procesando sección de pagos (Método: {})...", sale.getPaymentMethod());
            BigDecimal walletRedeemed = sale.getWalletRedeemed() != null ? sale.getWalletRedeemed() : BigDecimal.ZERO;
            if (walletRedeemed.compareTo(BigDecimal.ZERO) > 0) {
                ticket.append(utilsPOS.leftRightText("SUBTOTAL:", String.format("$%.2f", sale.getTotalAmount()), TICKET_WIDTH)).append("\n");
                ticket.append(utilsPOS.leftRightText("DESC. MONEDERO:", String.format("-$%.2f", walletRedeemed), TICKET_WIDTH)).append("\n");
                BigDecimal totalReal = sale.getTotalAmount().subtract(walletRedeemed);
                ticket.append(utilsPOS.leftRightText("TOTAL A PAGAR:", String.format("$%.2f", totalReal), TICKET_WIDTH)).append("\n");
            } else {
                ticket.append(utilsPOS.leftRightText("TOTAL A PAGAR:", String.format("$%.2f", sale.getTotalAmount()), TICKET_WIDTH)).append("\n");
            }
            switch (sale.getPaymentMethod()) {
                case CASH:
                    String recibido = sale.getAmountTendered() != null ? String.format("$%.2f", sale.getAmountTendered()) : "$0.00";
                    String cambio = sale.getChangeAmount() != null ? String.format("$%.2f", sale.getChangeAmount()) : "$0.00";
                    ticket.append(utilsPOS.leftRightText("EFECTIVO RECIBIDO:", recibido, TICKET_WIDTH)).append("\n");
                    ticket.append(utilsPOS.leftRightText("SU CAMBIO:", cambio, TICKET_WIDTH)).append("\n");
                    break;
                case CREDIT_CARD:
                case DEBIT_CARD:
                    String brand = sale.getCardBrand() != null ? sale.getCardBrand() : "TARJETA";
                    String last4 = sale.getLastFourDigits() != null ? "****" + sale.getLastFourDigits() : "";
                    String auth = sale.getAuthCode() != null ? sale.getAuthCode() : "APROBADO";
                    ticket.append(utilsPOS.leftRightText("PAGADO CON:", brand + " " + last4, TICKET_WIDTH)).append("\n");
                    ticket.append(utilsPOS.leftRightText("AUTORIZACION:", auth, TICKET_WIDTH)).append("\n");
                    ticket.append("\n");
                    ticket.append("\n").append(utilsPOS.centerText("PAGO EN UNA SOLA EXHIBICION", TICKET_WIDTH)).append("\n");
                    break;
                case TRANSFER:
                    String banco = sale.getCardBrand() != null ? sale.getCardBrand() : "SPEI";
                    String rastreo = sale.getAuthCode() != null ? sale.getAuthCode() : "N/A";
                    ticket.append(utilsPOS.leftRightText("PAGADO CON:", "TRANSFERENCIA " + banco, TICKET_WIDTH)).append("\n");
                    ticket.append(utilsPOS.leftRightText("CLAVE RASTREO:", rastreo, TICKET_WIDTH)).append("\n");
                    ticket.append("\n");
                    ticket.append("\n").append(utilsPOS.centerText("PAGO RECIBIDO VIA SPEI", TICKET_WIDTH)).append("\n");
                    break;
                case QR:
                    String plataforma = sale.getCardBrand() != null ? sale.getCardBrand() : "APP DIGITAL";
                    String operacion = sale.getAuthCode() != null ? sale.getAuthCode() : "N/A";
                    ticket.append(utilsPOS.leftRightText("PAGADO CON:", "CODIGO QR " + plataforma, TICKET_WIDTH)).append("\n");
                    ticket.append(utilsPOS.leftRightText("OPERACION:", operacion, TICKET_WIDTH)).append("\n");
                    ticket.append("\n");
                    ticket.append("\n").append(utilsPOS.centerText("PAGO POR MEDIOS DIGITALES", TICKET_WIDTH)).append("\n");
                    break;
            }
            // 6. SECCIÓN DE RECOMPENSAS / MONEDERO DIGITAL 🔥
            // Buscamos si en ESTA venta el cliente acumuló puntos usando el TransactionId
            Optional<WalletTransaction> earnedTx = walletTransactionRepository
                    .findByReferenceTicketAndTransactionType(sale.getTransactionId(), WalletTxType.ACCUMULATION);
            if (earnedTx.isPresent() || walletRedeemed.compareTo(BigDecimal.ZERO) > 0) {
                ticket.append(utilsPOS.divider(TICKET_WIDTH)).append("\n");
                ticket.append(utilsPOS.centerText("--- MONEDERO DIGITAL DAVHO'S ---", TICKET_WIDTH)).append("\n");
                if (walletRedeemed.compareTo(BigDecimal.ZERO) > 0) {
                    ticket.append(utilsPOS.leftRightText("Saldo Utilizado:", String.format("-$%.2f", walletRedeemed), TICKET_WIDTH)).append("\n");
                }
                if (earnedTx.isPresent()) {
                    ticket.append(utilsPOS.leftRightText("Puntos Ganados:", String.format("+$%.2f", earnedTx.get().getAmount()), TICKET_WIDTH)).append("\n");
                    ticket.append(utilsPOS.leftRightText("Saldo Disponible:", String.format("$%.2f", earnedTx.get().getWallet().getBalance()), TICKET_WIDTH)).append("\n");
                }
            }
            ticket.append(utilsPOS.asterisk(TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("Recuerda que puedes realizar", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("recargas de tiempo aire en todas", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("nuestras sucursales sin comision", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.asterisk(TICKET_WIDTH)).append("\n");
            // 6. PIE DE PÁGINA
            ticket.append(utilsPOS.divider(TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("¡Gracias por su compra!", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("Este ticket no es un", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("comprobante fiscal.", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.divider(TICKET_WIDTH)).append("\n");
            log.info("Contenido del ticket ensamblado. Guardando en base de datos...");
            SaleTicket document = SaleTicket.builder()
                    .sale(sale)
                    .ticketContent(ticket.toString())
                    .build();
            SaleTicket savedTicket = saleTicketRepository.save(document);
            log.info(ticket.toString());
            log.info("Ticket guardado exitosamente con ID: {}. Proceso finalizado.", savedTicket.getId());
            return new TicketResponse(ticket.toString(), sale.getTicketNumber(), "TICKET_SALE_".concat(sale.getId().toString()));
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

}
