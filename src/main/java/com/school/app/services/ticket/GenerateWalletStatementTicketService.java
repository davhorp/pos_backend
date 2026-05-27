package com.school.app.services.ticket;

import com.school.app.audit.Auditable;
import com.school.app.dto.response.TicketResponse;
import com.school.app.entity.*;
import com.school.app.enums.WalletTxType;
import com.school.app.repository.SaleRepository;
import com.school.app.repository.WalletRepository;
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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateWalletStatementTicketService {

    private final WalletRepository walletRepository;
    private final AuditLogService auditLogService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final SaleRepository saleRepository;
    private final UtilsPOS utilsPOS;
    // Ancho estándar para impresoras térmicas de 58mm
    private static final int TICKET_WIDTH = 47;

    /**
     * Genera un ticket de Estado de Cuenta del Monedero con el detalle de los productos.
     */
    @Auditable(action = SystemAuditLog.AuditAction.EDO_CTA_WALLET_CLIENT, entityName = "WALLET")
    @Transactional(readOnly = true)
    public TicketResponse generateWalletStatementTicket(String phoneNumber) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        long startTime = System.currentTimeMillis();
        boolean exito = true;
        String errorMsg = null;
        log.info("[WalletService] Iniciando generación de estado de cuenta. Cliente: {} | Solicitado por: {}",
                phoneNumber, request.getUserPrincipal().getName());
        try {
            Wallet wallet = walletRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> {
                        log.warn("[WalletService] Intento de consulta fallido: El monedero {} no existe.", phoneNumber);
                        return new IllegalArgumentException("Monedero no encontrado para el número: " + phoneNumber);
                    });
            // Buscamos los últimos 5 o 10 movimientos ordenados por fecha descendente
            log.info("[WalletService] Consultando historial de transacciones para monedero ID: {}", wallet.getId());
            List<WalletTransaction> transactions = walletTransactionRepository
                    .findTop10ByWalletOrderByCreatedAtDesc(wallet);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            log.info("[WalletService] Se recuperaron {} transacciones para el historial.", transactions.size());
            StringBuilder ticket = new StringBuilder();
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
            ticket.append(utilsPOS.centerText("PROGRAMA DE LEALTAD", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.divider(TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.leftRightText("TELEFONO:", phoneNumber, TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.leftRightText("SALDO ACTUAL:", String.format("$%.2f", wallet.getBalance()), TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.divider(TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("ULTIMOS MOVIMIENTOS", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.divider(TICKET_WIDTH)).append("\n");
            for (WalletTransaction tx : transactions) {
                String fecha = tx.getCreatedAt().format(formatter);
                String tipo = tx.getTransactionType() == WalletTxType.ACCUMULATION ? "SUMA" : "RESTA";
                String monto = String.format(tipo.equals("SUMA") ? "+$%.2f" : "-$%.2f", tx.getAmount());
                ticket.append(utilsPOS.leftRightText(fecha, monto, TICKET_WIDTH)).append("\n");
                // 🔥 LA MAGIA: Buscar qué compró con ese ticket
                Optional<Sale> saleOpt = saleRepository.findByTransactionId(tx.getReferenceTicket());
                if (saleOpt.isPresent()) {
                    Sale sale = saleOpt.get();
                    ticket.append("  TICKET: ").append(sale.getTransactionId()).append("\n");
                    // Iteramos sobre los productos de esa venta
                    for (SaleItem item : sale.getItems()) {
                        // Reutilizamos tu método formatProductLine que creamos antes
                        String productLine = "  * " + utilsPOS.formatProductLine(item);
                        ticket.append(productLine).append("\n");
                    }
                } else {
                    ticket.append("  MOVIMIENTO MANUAL / SISTEMA\n");
                }
                ticket.append(utilsPOS.divider(TICKET_WIDTH)).append("\n");
            }
            ticket.append("\n");
            ticket.append(utilsPOS.centerText("Este reporte es informativo.", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.centerText("Gracias por tu preferencia.", TICKET_WIDTH)).append("\n");
            ticket.append(utilsPOS.divider(TICKET_WIDTH)).append("\n\n");
            log.info("[WalletService] Estado de cuenta generado con éxito para {}. Longitud ticket: {} caracteres.",
                    phoneNumber, ticket.length());
            log.info(ticket.toString());
            return new TicketResponse(ticket.toString(), wallet.getId().toString(), "TICKET_EDO_WALLET_".concat(phoneNumber));
        } catch (IllegalArgumentException e) {
            exito = false;
            errorMsg = e.getMessage();
            log.warn("[WalletService] Error de negocio al generar estado de cuenta: {}", errorMsg);
            throw e;
        } catch (Exception e) {
            exito = false;
            errorMsg = e.getMessage();
            log.error("[WalletService] Error CRÍTICO al generar estado de cuenta para {}. Excepción: {}", phoneNumber, e.getMessage(), e);
            throw e;
        } finally {
            log.info("[WalletService] Finalización de proceso. Duración: {}ms | Resultado: {}",
                    System.currentTimeMillis() - startTime, exito ? "EXITO" : "FALLIDO");
            auditLogService.logActivity(
                    "EDO_CTA_WALLET_CLIENT",
                    "GenerateWalletStatementTicketService.generateWalletStatementTicket",
                    "Generación de Estado de cuenta (Monedero) del cliente: " + phoneNumber,
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
