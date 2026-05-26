package com.school.app.services.shift;


import com.school.app.audit.Auditable;
import com.school.app.dto.response.CashShiftResponse;
import com.school.app.entity.SystemAuditLog;
import com.school.app.entity.User;
import com.school.app.enums.ShiftStatus;
import com.school.app.repository.CashShiftRepository;
import com.school.app.services.auth.AuditLogService;
import com.school.app.utils.UtilsPOS;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class StatusCashShiftService {

    private final CashShiftRepository cashShiftRepository;
    private final UtilsPOS utilsPOS;
    private final AuditLogService auditLogService;

    /**
     * Consulta el estado actual de la caja para un usuario específico.
     * <p>
     * Busca en la base de datos si el usuario autenticado tiene un turno en estado {@code OPEN}.
     * Este método es el punto de entrada crítico para validar si el vendedor puede o no
     * operar el Punto de Venta (POS).
     * </p>
     *
     * @param currentUser El usuario (Vendedor/Cajero) actualmente autenticado.
     * @return Un {@link Optional} que contiene el DTO {@link CashShiftResponse} si existe
     *         un turno abierto, o un Optional vacío si la caja está cerrada.
     */
    @Auditable(action = SystemAuditLog.AuditAction.VERIFICAR_TURNO_ACTIVO, entityName = "CASH_SHIFT")
    public Optional<CashShiftResponse> getActiveShiftForUser(User currentUser) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        long startTime = System.currentTimeMillis();
        boolean exito = true;
        String mensajeError = null;
        Optional<CashShiftResponse> activeShift = Optional.empty();
        log.info("Iniciando validación de turno activo para el usuario con ID: {}", currentUser.getId());
        try {
            // 🔥 CORREGIDO: findByUserIdAndStatus para que coincida con la entidad
            activeShift = cashShiftRepository
                    .findByUserIdAndStatus(currentUser.getId(), ShiftStatus.OPEN)
                    .map(utilsPOS::mapToResponse);
            if (activeShift.isPresent()) {
                // 🔥 CORREGIDO: getShiftId() en base al DTO CashShiftResponse definido anteriormente
                log.info("Turno ABIERTO encontrado. Turno ID: {}", activeShift.get().id());
            } else {
                log.warn("El usuario ID: {} NO tiene ningún turno abierto en este momento.", currentUser.getId());
            }
        } catch (Exception e) {
            exito = false;
            mensajeError = e.getMessage();
            log.error("Error al consultar el turno activo del usuario ID {}: {}", currentUser.getId(), mensajeError);
            throw e;
        } finally {
            // 🔥 CORREGIDO: Ahora las variables exito y mensajeError existen y se evalúan correctamente
            auditLogService.logActivity(
                    "VERIFICAR_TURNO_ACTIVO",
                    "StatusCashShiftService.getActiveShiftForUser",
                    "Consulta de estado de caja. Turno encontrado: " + activeShift.isPresent(),
                    startTime,
                    exito,
                    mensajeError,
                    request.getRemoteAddr(),
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getUserPrincipal().getName());
        }
        return activeShift;
    }
}
