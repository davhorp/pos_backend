package com.school.app.services.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.app.audit.Auditable;
import com.school.app.dto.response.TicketResponse;
import com.school.app.entity.SystemAuditLog;
import com.school.app.entity.Wallet;
import com.school.app.repository.WalletRepository;
import com.school.app.services.auth.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletGetBalanceService {

    private final SpringTemplateEngine templateEngine;
    private final WalletRepository walletRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    /**
     * Genera el ticket de consulta de saldo del monedero.
     * @param phoneNumber El número de teléfono del cliente.
     * @return TicketResponse con el contenido HTML.
     */
    @Auditable(action = SystemAuditLog.AuditAction.CONSULTA_SALDO_WALLET, entityName = "WALLET")
    @Transactional(readOnly = true)
    public TicketResponse generateBalanceTicket(String phoneNumber) {
        log.info("[WalletService] Generando ticket de consulta de saldo para: {}", phoneNumber);
        // 1. Buscar el monedero
        Wallet wallet = walletRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> {
                    log.warn("[WalletService] No se encontró monedero para el teléfono: {}", phoneNumber);
                    return new IllegalArgumentException("Monedero no encontrado para el número: " + phoneNumber);
                });
        // 2. Preparar el contexto para Thymeleaf
        Context context = new Context();
        context.setVariable("wallet", wallet);
        context.setVariable("phoneNumber", phoneNumber);
        // 3. Generar el HTML procesando la plantilla "consulta-saldo"
        // Asegúrate de que el archivo se llame consulta-saldo.html en /templates
        String htmlContent = templateEngine.process("balance-wallet", context);
        log.info("[WalletService] Ticket de saldo generado exitosamente.");
        return new TicketResponse(
                htmlContent,
                wallet.getId().toString(),
                "TICKET_SALDO_".concat(phoneNumber)
        );
    }

    /**
     * Consulta el saldo actual de un monedero vinculado a un número de teléfono.
     * @param phone Número de teléfono a 10 dígitos.
     * @return El saldo disponible.
     */
    @Auditable(action = SystemAuditLog.AuditAction.CONSULTA_SALDO_WALLET, entityName = "WALLET")
    @Transactional(readOnly = true)
    public BigDecimal getBalanceByPhone(String phone) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        log.info("[WalletService] Iniciando consulta de saldo para el teléfono: {}", phone);
        long startTime = System.currentTimeMillis();
        boolean exito = false;
        String errorMsg = null;
        String detalles = "{}";
        try {
            // 1. Buscamos el monedero en la base de datos
            Wallet wallet = walletRepository.findByPhoneNumber(phone)
                    .orElseThrow(() -> new RuntimeException("No se encontró un monedero activo para el número: " + phone));
            BigDecimal currentBalance = wallet.getBalance();
            // 2. Preparamos los detalles en JSON para la auditoría
            detalles = objectMapper.writeValueAsString(Map.of(
                    "telefono", phone,
                    "saldo_consultado", currentBalance
            ));
            exito = true;
            log.info("[WalletService] Consulta exitosa. Saldo: ${}", currentBalance);
            return currentBalance;
        } catch (Exception e) {
            errorMsg = e.getMessage();
            log.error("[WalletService] Error al consultar el saldo del teléfono {}. Causa: {}", phone, errorMsg);
            // Construimos un JSON de error para que quede en el log
            try {
                detalles = objectMapper.writeValueAsString(Map.of("telefono", phone, "error", errorMsg));
            } catch (Exception ignored) {}
            throw new RuntimeException(errorMsg, e);
        } finally {
            // 3. Registro incondicional en la auditoría
            String username = (request.getUserPrincipal() != null) ? request.getUserPrincipal().getName() : "USUARIO_DESCONOCIDO";
            auditLogService.logActivity(
                    "CONSULTA_SALDO_MONEDERO",           // action
                    "WalletService.getBalanceByPhone",   // modulo
                    detalles,                            // detalles
                    startTime,                           // startTime
                    exito,                               // exito
                    errorMsg,                            // errorMsg
                    request.getRemoteAddr(),             // ip remota
                    request.getMethod(),                 // metodo http
                    request.getRequestURI(),             // uri
                    username                             // usuario extraído de forma segura
            );
        }
    }

}
