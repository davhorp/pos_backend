package com.school.app.repository;

import com.school.app.entity.Wallet;
import com.school.app.entity.WalletTransaction;
import com.school.app.enums.WalletTxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Optional<WalletTransaction> findTopByWalletOrderByCreatedAtDesc(Wallet wallet);

    Optional<WalletTransaction> findByReferenceTicketAndTransactionType(String referenceTicket, WalletTxType transactionType);

}
