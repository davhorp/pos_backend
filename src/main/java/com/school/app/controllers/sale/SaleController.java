package com.school.app.controllers.sale;

import com.school.app.dto.requets.SaleRequest;
import com.school.app.dto.response.ApiErrorResponse;
import com.school.app.dto.response.SaleResponse;
import com.school.app.entity.Sale;
import com.school.app.entity.User;
import com.school.app.repository.UserRepository;
import com.school.app.services.sale.SaleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Controlador REST encargado de gestionar las transacciones y pagos del Punto de Venta.
 *
 * Permite la creación de ventas (tickets) soportando múltiples métodos de pago
 * (Efectivo, Tarjeta de Crédito/Débito, Transferencia, QR).
 * Todo evento de cobro está estrictamente monitoreado y auditado.
 *
 * @author Jonathan David Reyes Ponce
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/pos/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final UserRepository userRepository;

    /**
     * Procesa el cobro de una venta y genera el ticket correspondiente.
     *
     * @param saleRequest Datos de la venta enviados por el frontend.
     * @param userDetails Usuario autenticado (cajero) inyectado por Spring Security.
     * @param httpRequest Objeto inyectado para capturar el path en caso de errores.
     * @return ResponseEntity con los detalles del ticket generado o ApiErrorResponse en caso de fallo.
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> processCheckout(
            @RequestBody SaleRequest saleRequest,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) { // 🔥 Inyectamos HttpServletRequest para el 'path'

        // 🔥 1. Validamos que el token realmente haya llegado
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new ApiErrorResponse(OffsetDateTime.now(), 401, "Unauthorized", "Falta el token o está expirado", httpRequest.getRequestURI())
            );
        }

        // 🔥 2. Buscamos tu Entidad real en la base de datos (usa findByEmail o findByUsername según tu repo)
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en la base de datos"));

        // A partir de aquí tu código sigue idéntico, ya no será null
        log.info("Iniciando proceso de cobro. Cajero: [{}]. Método de pago solicitado: [{}]",
                currentUser.getUsername(), saleRequest.paymentMethod());

        try {
            // 1. Validar que el método de pago sea válido según nuestro Enum
            Sale.PaymentMethod method;
            try {
                method = Sale.PaymentMethod.valueOf(saleRequest.paymentMethod().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Intento de cobro con método de pago inválido: [{}]", saleRequest.paymentMethod());

                // Construimos la nueva respuesta de error
                ApiErrorResponse errorResponse = new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Método de pago no soportado.",
                        httpRequest.getRequestURI()
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 2. Procesar la venta en el servicio (lógica transaccional auditada)
            log.info("Enviando payload al SaleService para validación de stock y creación de ticket...");
            SaleResponse response = saleService.processCheckout(saleRequest, currentUser, method);

            // 3. Log de éxito
            log.info("Cobro exitoso procesado. Ticket ID: [{}]. Monto Total: [{}]. Método: [{}]",
                    response.ticketId(), saleRequest.totalAmount(), method);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Manejo de errores de negocio (Ej. Stock insuficiente, sin turno abierto)
            log.warn("Regla de negocio no cumplida durante el cobro: {}", e.getMessage());

            ApiErrorResponse errorResponse = new ApiErrorResponse(
                    OffsetDateTime.now(),
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    e.getMessage(),
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            // Errores críticos (Ej. Fallo de conexión a BD)
            log.error("Error crítico al procesar el pago para el usuario [{}]: {}", currentUser.getUsername(), e.getMessage(), e);

            ApiErrorResponse errorResponse = new ApiErrorResponse(
                    OffsetDateTime.now(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    "Ocurrió un error interno al procesar el pago. Por favor contacte a soporte.",
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
