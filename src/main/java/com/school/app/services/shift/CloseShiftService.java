package com.school.app.services.shift;

import com.school.app.audit.Auditable;
import com.school.app.dto.requets.CloseShiftRequest;
import com.school.app.dto.response.CloseCashShiftResponse;
import com.school.app.entity.CashShift;
import com.school.app.entity.SystemAuditLog;
import com.school.app.enums.ShiftStatus;
import com.school.app.repository.CashShiftRepository;
import com.school.app.repository.SaleRepository;
import com.school.app.services.auth.AuditLogService;
import com.school.app.services.ticket.TicketCutBoxZService;
import com.school.app.utils.UtilsPOS;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class CloseShiftService {

    // Ancho estándar para impresoras térmicas de 58mm
    private static final int TICKET_WIDTH = 47;

    private final TicketCutBoxZService ticketCutBoxZService;
    private final CashShiftRepository cashShiftRepository;
    private final AuditLogService auditLogService;
    private final SaleRepository saleRepository;
    private final UtilsPOS utilsPOS;

    @Transactional
    @Auditable(action = SystemAuditLog.AuditAction.CLOSE_SHIFT, entityName = "CASH_SHIFT")
    public CloseCashShiftResponse closeShift(UUID shiftId, CloseShiftRequest request) {
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        long startTimeMillis = System.currentTimeMillis();
        boolean exito = true;
        String mensajeError = null;
        BigDecimal finalDiscrepancy = null; // Lo guardamos fuera del try para la auditoría
        log.info("[OPERACIÓN] Iniciando proceso de cierre de caja para el turno ID: {}", shiftId);
        try {
            // 1. Buscar turno abierto y validar
            CashShift shift = cashShiftRepository.findById(shiftId)
                    .orElseThrow(() -> {
                        log.error("[NEGOCIO FALLIDO] No se encontró un turno con ID {}", shiftId);
                        return new RuntimeException("No hay un turno válido para cerrar.");
                    });
            if (shift.getStatus() == ShiftStatus.CLOSED) {
                throw new RuntimeException("Este turno ya fue cerrado previamente.");
            }

            // 2. Inicializar contadores en cero
            BigDecimal cashSales = BigDecimal.ZERO;
            BigDecimal cardSales = BigDecimal.ZERO;
            BigDecimal transferSales = BigDecimal.ZERO;
            BigDecimal qrSales = BigDecimal.ZERO;
            BigDecimal totalSales = BigDecimal.ZERO;
            // 3. Obtener sumas agrupadas (¡Una sola consulta ultra rápida a la BD en lugar de 5!)
            log.debug("Calculando totales de ventas por método de pago para el turno ID: {}", shiftId);
            List<Object[]> sums = saleRepository.sumSalesByPaymentMethodAndShift(shiftId);
            for (Object[] row : sums) {
                if (row[0] == null || row[1] == null) continue;
                // Usamos toString() por si JPA devuelve un String o directamente el Enum PaymentMethod
                String method = row[0].toString();
                BigDecimal amount = (BigDecimal) row[1];
                totalSales = totalSales.add(amount);
                switch (method) {
                    case "CASH": cashSales = amount; break;
                    case "CREDIT_CARD":
                    case "DEBIT_CARD": cardSales = cardSales.add(amount); break;
                    case "TRANSFER": transferSales = amount; break;
                    case "QR": qrSales = amount; break;
                }
            }
            // 4. Matemáticas del arqueo
            BigDecimal startingCash = shift.getStartingCash();
            BigDecimal payouts = shift.getCashPayouts() != null ? shift.getCashPayouts() : BigDecimal.ZERO;
            // Lo que DEBERÍA haber físico = Fondo Inicial + Ventas en Efectivo - Retiros
            BigDecimal expectedCash = startingCash.add(cashSales).subtract(payouts);
            // Nota: Dependiendo de si tu Request es un Record o Clase, usa request.declaredCash() o getDeclaredCash()
            BigDecimal declaredCash = request.declaredCash();
            BigDecimal discrepancy = declaredCash.subtract(expectedCash);
            finalDiscrepancy = discrepancy;
            log.info("Arqueo Turno ID: {}. Esperado: {}, Declarado: {}, Diferencia: {}",
                    shiftId, expectedCash, declaredCash, discrepancy);
            // 5. Poblar la entidad con todos los datos calculados
            shift.setCashSales(cashSales);
            shift.setCardSales(cardSales);
            shift.setTransferSales(transferSales);
            shift.setQrSales(qrSales);
            shift.setTotalSales(totalSales);
            shift.setExpectedCash(expectedCash);
            shift.setDeclaredCash(declaredCash);
            shift.setDiscrepancyCash(discrepancy);
            shift.setEndTime(LocalDateTime.now());
            shift.setDiscrepancyReason(request.discrepancyReason());
            shift.setStatus(ShiftStatus.CLOSED);
            // 6. GENERAR Y GUARDAR EL TICKET HTML
            String ticketHtml = ticketCutBoxZService.generateZReportText(shift);
            log.info("TICKET CORTE Z: \n\n");
            log.info(ticketHtml);
            log.info("\n");
            shift.setClosingTicketHtml(ticketHtml);
            // 7. Guardar en base de datos
            cashShiftRepository.save(shift);
            log.info("[ÉXITO] Turno ID: {} cerrado exitosamente en la base de datos.", shiftId);
            // Devolvemos el DTO (Asegúrate de que mapToResponse también incluya el ticketHtml)
            return utilsPOS.mapToResponseCloseShift(shift);
        } catch (Exception e) {
            exito = false;
            mensajeError = e.getMessage();
            log.error("[ERROR CRÍTICO] Falló el proceso de cierre de caja para el turno ID: {}. Motivo: {}", shiftId, e.getMessage());
            throw e;
        } finally {
            // 8. Auditoría garantizada en el bloque finally
            long tiempoEjecucion = System.currentTimeMillis() - startTimeMillis;
            log.info("Finalizando ejecución de closeShift. Tiempo: {}ms. Registrando auditoría.", tiempoEjecucion);
            String auditDetails = String.format("Cierre ID: %s | Declarado: %s | Diferencia: %s",
                    shiftId,
                    request.declaredCash() != null ? request.declaredCash().toString() : "N/A",
                    finalDiscrepancy != null ? finalDiscrepancy.toString() : "FALLIDO");
            // Usamos tu método optimizado de 6 parámetros (sin necesidad de pasar el request web)
            auditLogService.logActivity(
                    "CERRAR_CAJA",
                    "ShiftService.closeShift",
                    auditDetails,
                    startTimeMillis,
                    exito,
                    mensajeError,
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getMethod(),
                    httpServletRequest.getRequestURI(),
                    httpServletRequest.getUserPrincipal().getName());
        }
    }

}
