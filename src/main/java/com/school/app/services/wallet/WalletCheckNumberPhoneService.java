package com.school.app.services.wallet;

import com.school.app.audit.Auditable;
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

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletCheckNumberPhoneService {

    private final WalletRepository walletRepository;
    private final AuditLogService auditLogService;

    @Auditable(action = SystemAuditLog.AuditAction.CONSULTAR_MONEDERO, entityName = "WALLETS")
    @Transactional(readOnly = true)
    public BigDecimal getBalanceByPhoneNumber(String phoneNumber) {
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        long startTime = System.currentTimeMillis();
        boolean exito = false;
        String errorMsg = null;
        log.info("[WalletService] Iniciando consulta de saldo para el número: {}", phoneNumber);
        try {
            Optional<Wallet> walletOpt = walletRepository.findByPhoneNumber(phoneNumber);
            BigDecimal balance = walletOpt.map(Wallet::getBalance).orElse(BigDecimal.ZERO);
            exito = true;
            log.info("[WalletService] Consulta exitosa. Saldo: ${} para el número: {}", balance, phoneNumber);
            return balance;
        } catch (Exception e) {
            errorMsg = e.getMessage();
            log.error("[WalletService] Error al consultar saldo para {}. Causa: {}", phoneNumber, errorMsg, e);
            throw e; // O lanza tu excepción personalizada (ej. PosBusinessException)
        } finally {
            auditLogService.logActivity(
                    "CONSULTAR_MONEDERO",
                    "WalletService.getBalanceByPhoneNumber",
                    "Consulta de saldo para teléfono: " + phoneNumber,
                    startTime,
                    exito,
                    errorMsg,
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getMethod(),
                    httpServletRequest.getRequestURI(),
                    httpServletRequest.getUserPrincipal().getName()
            );
        }
    }
}
