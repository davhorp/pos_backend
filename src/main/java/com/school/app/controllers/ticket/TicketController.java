package com.school.app.controllers.ticket;

import com.school.app.dto.response.ApiErrorResponse;
import com.school.app.entity.User;
import com.school.app.repository.UserRepository;
import com.school.app.services.ticket.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Controlador REST {@Link TicketController} encargado de la generación y emisión de tickets de venta.
 * Permite obtener el formato crudo (plain text) de una venta para ser
 * enviado directamente a una impresora térmica por el cliente (frontend).
 *
 * @author Jonathan David Reyes Ponce
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/pos/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final UserRepository userRepository; // Para buscar al usuario autenticado

    /**
     * Genera el formato de texto para la impresión del ticket de una venta específica.
     * La anotación @Auditable registrará qué usuario imprimió el ticket y a qué hora.
     *
     * @param saleId UUID de la venta.
     * @param userDetails Usuario autenticado que solicita la impresión.
     * @param httpRequest Contexto de la petición para auditoría y manejo de errores.
     * @return TicketResponse con el texto crudo listo para impresora térmica térmica.
     */
    @GetMapping("/{saleId}")
    public ResponseEntity<?> generateTicket(
            @PathVariable UUID saleId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        log.info("Solicitud de impresión de ticket recibida. Venta ID: [{}]. Usuario: [{}]",
                saleId, userDetails != null ? userDetails.getUsername() : "ANÓNIMO");
        try {
            // Validar autenticación
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ApiErrorResponse(OffsetDateTime.now(), 401, "Unauthorized", "Falta token de seguridad.", httpRequest.getRequestURI())
                );
            }
            // Validar que el usuario exista
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado en la base de datos"));
            log.info("Construyendo formato térmico para la impresora...");
            // Generar el contenido del ticket
           // String ticketContent = ticketService.generateThermalTicket(saleId);
            log.info("Ticket generado exitosamente para la Venta ID: [{}].", saleId);
            return ResponseEntity.ok(ticketService.generateThermalTicket(saleId));
        } catch (IllegalArgumentException e) {
            log.warn("Intento de imprimir ticket de una venta inexistente. ID: [{}]. Motivo: {}", saleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ApiErrorResponse(OffsetDateTime.now(), 404, "Not Found", e.getMessage(), httpRequest.getRequestURI())
            );
        } catch (Exception e) {
            log.error("Error crítico al generar ticket para la venta [{}]: {}", saleId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiErrorResponse(OffsetDateTime.now(), 500, "Internal Server Error", "Error al formatear el ticket.", httpRequest.getRequestURI())
            );
        }
    }
}
