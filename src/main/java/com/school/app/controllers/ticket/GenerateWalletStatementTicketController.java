package com.school.app.controllers.ticket;

import com.school.app.dto.response.TicketResponse;
import com.school.app.services.ticket.GenerateWalletStatementTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/pos/wallets")
@RequiredArgsConstructor
public class GenerateWalletStatementTicketController {

    private final GenerateWalletStatementTicketService generateWalletStatementTicketService;

    /**
     * Genera el ticket térmico con el estado de cuenta y últimos movimientos del monedero.
     * * @param phoneNumber Número de celular del cliente (10 dígitos).
     * @return TicketResponse con el contenido en texto plano formateado.
     */
    @GetMapping("/{phoneNumber}/statement/ticket")
    public ResponseEntity<TicketResponse> getWalletStatementTicket(@PathVariable String phoneNumber) {
        log.info("Solicitud REST recibida para generar ticket de estado de cuenta. Teléfono: {}", phoneNumber);
        try {
            TicketResponse response = generateWalletStatementTicketService.generateWalletStatementTicket(phoneNumber);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Error al generar estado de cuenta: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error interno generando el ticket del monedero", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
