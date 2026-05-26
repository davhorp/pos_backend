package com.school.app.controllers.dashboard;

import com.school.app.dto.response.DashboardResponse;
import com.school.app.services.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controlador REST para manejar las peticiones del Dashboard Administrativo.
 * Proporciona endpoints para recuperar métricas financieras y operativas del negocio.
 */
@Slf4j
@RestController
@RequestMapping("/api/pos/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Recupera las estadísticas financieras para un rango de fechas específico.
     * Si no se envían fechas, por defecto calcula las ventas del día actual.
     *
     * @param startDate Fecha de inicio del periodo (Formato ISO: YYYY-MM-DD).
     * @param endDate   Fecha de fin del periodo (Formato ISO: YYYY-MM-DD).
     * @return ResponseEntity con las métricas estructuradas en {@link DashboardResponse}.
     */
    @GetMapping("/admin-stats")
    //@PreAuthorize("hasRole('ADMIN')") // 🔥 Seguridad: Solo administradores
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<DashboardResponse> getAdminStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // Si el frontend no manda fechas (por ejemplo, al recargar la página), tomamos "Hoy" por defecto
        if (startDate == null || endDate == null) {
            startDate = LocalDate.now();
            endDate = LocalDate.now();
            log.debug("No se proporcionaron fechas en la petición. Se utilizará la fecha actual: {}", startDate);
        }
        DashboardResponse stats = dashboardService.getAdminDashboardStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }
}
