package com.school.app.services.dashboard;

import com.school.app.audit.Auditable;
import com.school.app.dto.response.DashboardResponse;
import com.school.app.dto.response.PaymentMethodResponse;
import com.school.app.dto.response.SalesMetricResponse;
import com.school.app.entity.SystemAuditLog;
import com.school.app.repository.CashShiftRepository;
import com.school.app.repository.SaleRepository;
import com.school.app.services.auth.AuditLogService;
import com.school.app.utils.UtilsPOS;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio encargado de procesar la lógica de negocio y cálculos matemáticos
 * para el Dashboard Financiero de Elotiuz.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final UtilsPOS utilsPOS;
    private final CashShiftRepository cashShiftRepository;
    private final AuditLogService auditLogService;

    /**
     * Calcula las métricas de ventas comparando el periodo solicitado con el periodo
     * inmediatamente anterior de la misma longitud en días.
     *
     * @param startDate Fecha inicial solicitada por el usuario.
     * @param endDate   Fecha final solicitada por el usuario.
     * @return {@link DashboardResponse} con la lista de métricas financieras calculadas.
     */
    @Auditable(action = SystemAuditLog.AuditAction.VIEW_ADMIN_DASHBOARD, entityName = "PRODUCT")
    @Transactional(readOnly = true)
    public DashboardResponse getAdminDashboardStats(LocalDate startDate, LocalDate endDate) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        // LOG INFO: Indica que un proceso de negocio importante ha comenzado
        log.info("Iniciando motor de cálculo financiero. Período base: {} a {}", startDate, endDate);
        long startTime = System.currentTimeMillis();
        // 1. Calcular la longitud del periodo (ej: Si eligen 1 semana, son 7 días)
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        // 2. Definir las fechas del "Periodo Anterior" para la comparación (%)
        LocalDate previousStartDate = startDate.minusDays(daysBetween);
        LocalDate previousEndDate = startDate.minusDays(1);
        log.info("Período de comparación calculado ({} días): {} a {}", daysBetween, previousStartDate, previousEndDate);
        // 🔥 Variables de control para la auditoría
        boolean exito = false;
        String mensajeError = null;
        String detallesAuditoria = "Inició la consulta del Dashboard Financiero.";
        try {
            // 3. Convertir LocalDates a OffsetDateTime con la zona horaria del servidor (Vital para PostgreSQL)
            ZoneId zoneId = ZoneId.systemDefault();
            // Rango Actual: Desde las 00:00:00 del startDate, hasta las 00:00:00 del día DESPUÉS de endDate
            OffsetDateTime currentStart = startDate.atStartOfDay(zoneId).toOffsetDateTime();
            OffsetDateTime currentEnd = endDate.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();
            // Rango Anterior
            OffsetDateTime prevStart = previousStartDate.atStartOfDay(zoneId).toOffsetDateTime();
            OffsetDateTime prevEnd = previousEndDate.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();
            // ==========================================
            // 4. EJECUTAR CONSULTAS EN BASE DE DATOS
            // ==========================================
            // Ventas
            BigDecimal currentSales = saleRepository.sumTotalSalesBetweenDates(currentStart, currentEnd);
            BigDecimal previousSales = saleRepository.sumTotalSalesBetweenDates(prevStart, prevEnd);
            double salesChange = utilsPOS.calculatePercentageChange(currentSales, previousSales);
            // Tickets Emitidos
            long currentTicketsCount = saleRepository.countSalesBetweenDates(currentStart, currentEnd);
            long previousTicketsCount = saleRepository.countSalesBetweenDates(prevStart, prevEnd);
            BigDecimal currentTickets = new BigDecimal(currentTicketsCount);
            BigDecimal previousTickets = new BigDecimal(previousTicketsCount);
            double ticketsChange = utilsPOS.calculatePercentageChange(currentTickets, previousTickets);
            // Ticket Promedio
            BigDecimal currentAvgTicket = currentTicketsCount > 0
                    ? currentSales.divide(currentTickets, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal previousAvgTicket = previousTicketsCount > 0
                    ? previousSales.divide(previousTickets, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            double avgTicketChange = utilsPOS.calculatePercentageChange(currentAvgTicket, previousAvgTicket);
            // 🔥 NUEVO: Consultar desglose por método de pago
            List<Object[]> rawPaymentMethods = saleRepository.sumTotalSalesByPaymentMethodBetweenDates(currentStart, currentEnd);
            List<PaymentMethodResponse> paymentMethods = mapPaymentMethodsToResponse(rawPaymentMethods, currentStart, currentEnd);
            log.info("Cálculos de base de datos finalizados con éxito.");
            // ==========================================
            // 5. ENSAMBLAR LA RESPUESTA
            // ==========================================
            List<SalesMetricResponse> metricsList = List.of(
                    new SalesMetricResponse("Ventas Totales", currentSales, salesChange, "📈", true),
                    new SalesMetricResponse("Tickets Emitidos", currentTickets, ticketsChange, "🧾", false),
                    new SalesMetricResponse("Ticket Promedio", currentAvgTicket, avgTicketChange, "🛒", true)
            );
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("✅ Reporte de Dashboard generado en {} ms. Total procesado: ${}", executionTime, currentSales);
            // Actualizamos las variables de auditoría para el caso de éxito
            detallesAuditoria = String.format("Consultó el Dashboard Financiero. Ventas registradas hoy: $%s", currentSales);
            exito = true;
            return new DashboardResponse(metricsList, paymentMethods);
        } catch (Exception e) {
            log.error("Fallo crítico al generar las métricas del dashboard: {}", e.getMessage(), e);
            // Actualizamos las variables de auditoría para el caso de error
            detallesAuditoria = "Error al intentar cargar las métricas financieras.";
            mensajeError = e.getMessage();
            exito = false;
            throw e;
        } finally {
            // 🔥 REGISTRO DE AUDITORÍA CENTRALIZADO
            // Se ejecutará SIEMPRE, ya sea que el try haga 'return' o el catch haga 'throw'
            auditLogService.logActivity(
                    "CONSULTAR_DASHBOARD",
                    "DashboardService.getAdminDashboardStats",
                    detallesAuditoria,
                    startTime,
                    exito,
                    mensajeError,
                    request.getRemoteAddr(),
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "SISTEMA"
            );
        }
    }

    /**
     * Mapea los resultados crudos de SQL a la estructura requerida por Angular.
     * Garantiza que todos los métodos de pago existan en la respuesta (con valor $0.00 si no hubo ventas).
     */
    private List<PaymentMethodResponse> mapPaymentMethodsToResponse(
            List<Object[]> rawResults,
            OffsetDateTime startDate,
            OffsetDateTime endDate) { // 🔥 Nuevos parámetros recibidos
        // Inicializamos los acumuladores en CERO por seguridad
        Map<String, BigDecimal> amounts = new HashMap<>();
        amounts.put("EFECTIVO", BigDecimal.ZERO);
        amounts.put("TARJETA", BigDecimal.ZERO);
        amounts.put("SPEI", BigDecimal.ZERO);
        amounts.put("QR", BigDecimal.ZERO);
        // (Opcional) Ejemplo de cómo podrías usar las fechas aquí adentro:
        log.info("Procesando métodos de pago del periodo {} al {}", startDate, endDate);
        // Llenamos con los datos reales de la base de datos
        for (Object[] row : rawResults) {
            if (row[0] != null) {
                String method = row[0].toString();
                BigDecimal total = (BigDecimal) row[1];
                amounts.put(method, total);
            }
        }
        // Construimos la lista final
        return List.of(
                new PaymentMethodResponse("Efectivo", amounts.getOrDefault("EFECTIVO", BigDecimal.ZERO)
                        .add(amounts.getOrDefault("CASH", BigDecimal.ZERO)), "#10b981"),
                new PaymentMethodResponse("Tarjeta (Terminal)", amounts.getOrDefault("TARJETA", BigDecimal.ZERO)
                        .add(amounts.getOrDefault("CREDIT_CARD", BigDecimal.ZERO))
                        .add(amounts.getOrDefault("DEBIT_CARD", BigDecimal.ZERO)), "#3b82f6"),
                new PaymentMethodResponse("Transferencia (SPEI)", amounts.getOrDefault("SPEI", BigDecimal.ZERO)
                        .add(amounts.getOrDefault("TRANSFER", BigDecimal.ZERO)),"#8b5cf6"),
                new PaymentMethodResponse("Código QR (Apps)", amounts.get("QR"), "#f59e0b")
        );
    }

}
