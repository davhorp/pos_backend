package com.school.app.services.wallet;

import com.school.app.audit.Auditable;
import com.school.app.entity.SystemAuditLog;
import com.school.app.entity.Wallet;
import com.school.app.entity.WalletTransaction;
import com.school.app.enums.WalletTxType;
import com.school.app.repository.WalletRepository;
import com.school.app.repository.WalletTransactionRepository;
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
public class WalletDeductBalanceService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Auditable(action = SystemAuditLog.AuditAction.DEDUCIR_SALDO_WALLET, entityName = "WALLETS")
    @Transactional
    public void deductBalance(String phoneNumber, BigDecimal amountToRedeem, String currentTicketNumber) {
        // 1. Buscamos el monedero o lo creamos desde cero si no existe
        Wallet wallet = walletRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    log.info("[WalletService] Creando nuevo monedero para el número: {}", phoneNumber);
                    Wallet newWallet = Wallet.builder()
                            .phoneNumber(phoneNumber)
                            .balance(BigDecimal.ZERO)
                            .build();
                    return walletRepository.save(newWallet);
                });
        // 2. Validación de fondos (Si se acaba de crear, esto fallará correctamente)
        if (wallet.getBalance().compareTo(amountToRedeem) < 0) {
            throw new IllegalStateException("Saldo insuficiente en el monedero para el número " + phoneNumber);
        }
        // 3. Restamos el monto
        wallet.setBalance(wallet.getBalance().subtract(amountToRedeem));
        walletRepository.save(wallet);
        // 4. Registramos el movimiento histórico apuntando al ticket ACTUAL
        WalletTransaction walletTransactionRedemption = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amountToRedeem)
                .transactionType(WalletTxType.REDEMPTION)
                .referenceTicket(currentTicketNumber) // Usamos el folio de la venta en curso
                .build();
        walletTransactionRepository.save(walletTransactionRedemption);
        log.info("✅ [WalletService] Se redimieron ${} del número {}. Nuevo saldo: ${} | Ticket: {}",
                amountToRedeem, phoneNumber, wallet.getBalance(), currentTicketNumber);
    }

}
