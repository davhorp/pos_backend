package com.school.app.controllers.wallet;

import com.school.app.dto.response.WalletCheckResponse;
import com.school.app.services.wallet.WalletCheckNumberPhoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/pos/wallets")
@RequiredArgsConstructor
public class WalletCheckNumberPhoneController {

    private final WalletCheckNumberPhoneService walletCheckNumberPhoneService;

    /**
     * Verifica el saldo disponible en el monedero electrónico de un cliente.
     * Path: GET /api/pos/wallets/check?phoneNumber=5512345678
     *
     * @param phoneNumber El número de celular a 10 dígitos del cliente.
     * @return WalletCheckResponse con el saldo actual.
     */
    @GetMapping("/check")
    public ResponseEntity<WalletCheckResponse> checkWalletBalance(
            @RequestParam(name = "phoneNumber") String phoneNumber) {
        // Delegamos la lógica de búsqueda al servicio
        BigDecimal currentBalance = walletCheckNumberPhoneService.getBalanceByPhoneNumber(phoneNumber);
        // Instanciamos el record nativo directamente con 'new'
        WalletCheckResponse response = new WalletCheckResponse(currentBalance);
        return ResponseEntity.ok(response);
    }
}
