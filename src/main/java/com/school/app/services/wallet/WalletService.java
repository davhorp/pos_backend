package com.school.app.services.wallet;

import com.school.app.audit.Auditable;
import com.school.app.dto.requets.ProductSummaryDto;
import com.school.app.dto.requets.TransactionDetailDto;
import com.school.app.dto.requets.WalletStatementDto;
import com.school.app.entity.*;
import com.school.app.enums.WalletTxType;
import com.school.app.repository.SaleRepository;
import com.school.app.repository.WalletRepository;
import com.school.app.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio encargado de gestionar los saldos y transacciones del monedero electrónico.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final SaleRepository saleRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

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
        log.info("[WalletService] Iniciando proceso de acumulación de monedero para el teléfono: {} desde el ticket: {}", phoneNumber, ticketNumber);
        // 1. Validaciones iniciales de seguridad
        if (phoneNumber == null || phoneNumber.trim().isBlank()) {
            log.warn("[WalletService] No se proporcionó número telefónico. Se omite el abono al monedero.");
            return BigDecimal.ZERO;
        }
        if (purchaseTotal == null || purchaseTotal.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[WalletService] Monto de compra inválido o nulo (${}). Proceso cancelado.", purchaseTotal);
            return BigDecimal.ZERO;
        }
        // 2. Calcular el monto a otorgar (Redondeado a 2 decimales hacia arriba/mitad)
        BigDecimal amountToEarn = purchaseTotal.multiply(ACCUMULATION_RATE).setScale(2, RoundingMode.HALF_UP);
        if (amountToEarn.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[WalletService] El monto calculado para acumular es menor o igual a cero (${}). Proceso cancelado.", amountToEarn);
            return BigDecimal.ZERO;
        }
        // 3. Obtener el monedero existente o crear uno nuevo si es su primera interacción
        Wallet wallet = walletRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    log.info("[WalletService] Monedero no encontrado para el teléfono: {}. Registrando nueva cuenta digital.", phoneNumber);
                    Wallet newWallet = Wallet.builder()
                            .phoneNumber(phoneNumber)
                            .balance(BigDecimal.ZERO)
                            .build();
                    return walletRepository.save(newWallet);
                });
        // 4. Incrementar el saldo
        BigDecimal oldBalance = wallet.getBalance();
        wallet.setBalance(oldBalance.add(amountToEarn));
        walletRepository.save(wallet);
        // 5. Registrar auditoría histórica del movimiento
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amountToEarn)
                .transactionType(WalletTxType.ACCUMULATION) // Tu Enum de tipo de transacción
                .referenceTicket(ticketNumber)
                .build();
        this.generateWalletTransacction(transaction);
        log.info("✅ Monedero actualizado con éxito. Teléfono: {} | Abonado: ${} | Saldo Anterior: ${} | Nuevo Saldo: ${}",
                phoneNumber, amountToEarn, oldBalance, wallet.getBalance());
        return amountToEarn;
    }

    @Auditable(action = SystemAuditLog.AuditAction.MONEDERO_TRANSACCION, entityName = "WALLET_TRANSACTIONS")
    private void generateWalletTransacction(WalletTransaction transaction){
        walletTransactionRepository.save(transaction);
    }

    /**
     * Recopila y estructura toda la información del monedero de un cliente,
     * incluyendo el desglose de productos comprados en cada movimiento,
     * ideal para exportación a PDF o reportes detallados.
     *
     * @param phoneNumber Número de celular del cliente.
     * @return WalletStatementDto con los datos estructurados.
     */
    @Transactional(readOnly = true)
    @Auditable(action = SystemAuditLog.AuditAction.CONSULTA_MONEDERO, entityName = "WALLETS") // Ajusta el Enum según tu clase SystemAuditLog
    public WalletStatementDto getStatementData(String phoneNumber) {
        log.info("[WalletService] Iniciando recopilación de datos para estado de cuenta. Cliente: {}", phoneNumber);
        long startTime = System.currentTimeMillis();
        try {
            // 1. Validación y extracción del monedero base
            Wallet wallet = walletRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> {
                        log.warn("[WalletService] Intento de consulta fallido: Monedero no encontrado para {}", phoneNumber);
                        return new IllegalArgumentException("No existe un monedero asociado al número " + phoneNumber);
                    });
            // 2. Extraer historial (Usamos el mismo método del ticket térmico)
            log.info("[WalletService] Consultando historial de transacciones para monedero ID: {}", wallet.getId());
            List<WalletTransaction> transactions = walletTransactionRepository.findTop10ByWalletOrderByCreatedAtDesc(wallet);
            log.info("[WalletService] Se recuperaron {} transacciones para procesar.", transactions.size());
            // 3. Mapear transacciones y buscar detalle de productos
            List<TransactionDetailDto> transactionDtos = new ArrayList<>();
            for (WalletTransaction tx : transactions) {
                List<ProductSummaryDto> productDtos = new ArrayList<>();
                // Si la transacción tiene un folio de venta, cruzamos la información con SaleRepository
                if (tx.getReferenceTicket() != null && !tx.getReferenceTicket().trim().isEmpty()) {
                    Optional<Sale> saleOpt = saleRepository.findByTransactionId(tx.getReferenceTicket());
                    if (saleOpt.isPresent()) {
                        Sale sale = saleOpt.get();
                        // Convertir los artículos de la venta al sub-DTO
                        for (SaleItem item : sale.getItems()) {
                            productDtos.add(new ProductSummaryDto(
                                    item.getProduct().getName(),
                                    item.getQuantity(),
                                    item.getUnitPrice()
                            ));
                        }
                    } else {
                        log.info("[WalletService] Transacción {}: El ticket {} no se encontró en la tabla de Ventas.",
                                tx.getId(), tx.getReferenceTicket());
                    }
                }
                // Definir texto amigable para el PDF
                String tipoMovimiento = tx.getTransactionType() == WalletTxType.ACCUMULATION ? "BONIFICACIÓN" : "CARGO";
                // Agregar el movimiento a la lista final
                transactionDtos.add(new TransactionDetailDto(
                        tx.getCreatedAt(),
                        tipoMovimiento,
                        tx.getAmount(),
                        tx.getReferenceTicket() != null ? tx.getReferenceTicket() : "MOVIMIENTO INTERNO",
                        productDtos
                ));
            }
            WalletStatementDto statementData = new WalletStatementDto(
                    wallet.getPhoneNumber(),
                    "N/A",
                    wallet.getBalance(),
                    LocalDateTime.now(),
                    transactionDtos
            );
            // 5. Métricas y finalización
            long duration = System.currentTimeMillis() - startTime;
            log.info("[WalletService] Estado de cuenta generado exitosamente. Teléfono: {} | Tiempo de proceso: {}ms",
                    phoneNumber, duration);
            return statementData;
        } catch (IllegalArgumentException e) {
            // Se relanza tal cual para que el controlador devuelva un 400 Bad Request
            throw e;
        } catch (Exception e) {
            // Atrapa errores de BD o Nulos inesperados
            long duration = System.currentTimeMillis() - startTime;
            log.error("[WalletService] Error CRÍTICO generando estado de cuenta para {}. Tiempo: {}ms. Excepción: {}",
                    phoneNumber, duration, e.getMessage(), e);
            throw new RuntimeException("Error interno al procesar los datos del monedero.");
        }
    }
}
