package com.school.app.repository;

import com.school.app.entity.Wallet;
import com.school.app.entity.WalletTransaction;
import com.school.app.enums.WalletTxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    /**
     * Trae los últimos N movimientos de un monedero (útil para el estado de cuenta).
     */
    List<WalletTransaction> findTop10ByWalletOrderByCreatedAtDesc(Wallet wallet);

    /**
     * Busca una transacción por ticket y por tipo (útil para saber cuántos puntos
     * se ganaron exactamente en una venta específica).
     */
    Optional<WalletTransaction> findByReferenceTicketAndTransactionType(String referenceTicket, WalletTxType transactionType);

}
