package com.school.app.controllers.wallet;

import com.school.app.dto.response.WalletBalanceResponse;
import com.school.app.services.wallet.WalletGetBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/pos/wallets")
@RequiredArgsConstructor
public class WalletGetBalanceController {

    private final WalletGetBalanceService walletGetBalanceService;

    /**
     * Endpoint para consultar el saldo de un cliente.
     * GET /api/pos/wallets/{phone}/balance
     */
    @GetMapping("/{phone}/balance")
    public ResponseEntity<WalletBalanceResponse> getWalletBalance(@PathVariable String phone) {
        log.info("[WalletController] Petición REST recibida para consultar saldo del teléfono: {}", phone);
        BigDecimal balance = walletGetBalanceService.getBalanceByPhone(phone);
        return ResponseEntity.ok(new WalletBalanceResponse(phone, walletGetBalanceService.generateBalanceTicket(phone).ticketContent(), balance));
    }
}
