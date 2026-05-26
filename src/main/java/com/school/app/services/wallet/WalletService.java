package com.school.app.services.wallet;

import com.school.app.audit.Auditable;
import com.school.app.entity.SystemAuditLog;
import com.school.app.entity.Wallet;
import com.school.app.entity.WalletTransaction;
import com.school.app.enums.WalletTxType;
import com.school.app.repository.WalletRepository;
import com.school.app.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Servicio encargado de gestionar los saldos y transacciones del monedero electrónico.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    // Tasa de acumulación: 2% del total de la compra (ej: Compra de $100 acumula $2.00)
    private static final BigDecimal ACCUMULATION_RATE = new BigDecimal("0.02");

    /**
     * Calcula y abona centavos/pesos a la cuenta digital de un cliente basado en el total de su compra.
     * Si el monedero no existe para ese número telefónico, se crea automáticamente.
     *
     * @param phoneNumber  Número telefónico de identificación del cliente.
     * @param purchaseTotal Monto total del ticket de compra.
     * @param ticketNumber  Folio del ticket de venta para vinculación y auditoría.
     * @return El monto neto en dinero que fue abonado al monedero.
     */
    @Transactional
    @Auditable(action = SystemAuditLog.AuditAction.ABONO_MONEDERO, entityName = "WALLETS")
    public BigDecimal accumulateBalance(String phoneNumber, BigDecimal purchaseTotal, String ticketNumber) {
        log.info("Iniciando proceso de acumulación de monedero para el teléfono: {} desde el ticket: {}", phoneNumber, ticketNumber);
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.debug("No se proporcionó número telefónico. Se omite el abono al monedero.");
            return BigDecimal.ZERO;
        }
        // 1. Calcular el monto a otorgar (Redondeado a 2 decimales hacia arriba/mitad)
        BigDecimal amountToEarn = purchaseTotal.multiply(ACCUMULATION_RATE).setScale(2, RoundingMode.HALF_UP);
        if (amountToEarn.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("El monto calculado para acumular es menor o igual a cero (${}). Proceso cancelado.", amountToEarn);
            return BigDecimal.ZERO;
        }
        // 2. Obtener el monedero existente o crear uno nuevo si es su primera interacción
        Wallet wallet = walletRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    log.info("Monedero no encontrado para el teléfono: {}. Registrando nueva cuenta digital.", phoneNumber);
                    Wallet newWallet = new Wallet();
                    newWallet.setPhoneNumber(phoneNumber);
                    newWallet.setBalance(BigDecimal.ZERO);
                    return walletRepository.save(newWallet);
                });
        // 3. Incrementar el saldo
        BigDecimal oldBalance = wallet.getBalance();
        wallet.setBalance(oldBalance.add(amountToEarn));
        walletRepository.save(wallet);
        // 4. Registrar auditoría histórica del movimiento
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amountToEarn);
        transaction.setTransactionType(WalletTxType.ACCUMULATION);
        transaction.setReferenceTicket(ticketNumber);
        this.generateWalletTransacction(transaction);
        log.info("✅ Monedero actualizado con éxito. Teléfono: {} | Abonado: ${} | Saldo Anterior: ${} | Nuevo Saldo: ${}",
                phoneNumber, amountToEarn, oldBalance, wallet.getBalance());
        return amountToEarn;
    }

    @Auditable(action = SystemAuditLog.AuditAction.TICKET_MONEDERO, entityName = "WALLET_TRANSACTIONS")
    private void generateWalletTransacction(WalletTransaction transaction){
        transactionRepository.save(transaction);

    }
}
