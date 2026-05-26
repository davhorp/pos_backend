package com.school.app.services.shift;

import com.school.app.audit.Auditable;
import com.school.app.dto.response.CashShiftResponse;
import com.school.app.entity.AuditLog;
import com.school.app.entity.CashShift;
import com.school.app.entity.SystemAuditLog;
import com.school.app.entity.User;
import com.school.app.enums.ShiftStatus;
import com.school.app.repository.AuditLogRepository;
import com.school.app.repository.CashShiftRepository;
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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class OpenShiftService {

    private final CashShiftRepository cashShiftRepository;
    private final UtilsPOS utilsPOS;
    private final AuditLogService auditLogService;

    /**
     * Abre un nuevo turno de caja para el usuario especificado.
     * <p>
     * Este método valida como regla de negocio estricta que el usuario no posea
     * actualmente un turno en estado {@code OPEN}. Si la validación es exitosa,
     * inicializa un nuevo turno con el fondo proporcionado y lo persiste en la base de datos.
     * </p>
     *
     * @param currentUser    El usuario autenticado que está abriendo el turno.
     * @param openingBalance El fondo inicial (morralla) en efectivo con el que arranca la caja.
     * @return Un DTO {@link CashShiftResponse} con la información del turno creado.
     * @throws IllegalStateException Si el usuario ya cuenta con un turno de caja en estado abierto.
     */
    @Auditable(action = SystemAuditLog.AuditAction.OPEN_SHIFT, entityName = "CASH_SHIFT")
    @Transactional
    public CashShiftResponse openShift(User currentUser, BigDecimal openingBalance) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        long startTimeMillis = System.currentTimeMillis();
        boolean exito = true;
        String mensajeError = null;
        log.info("[OPERACIÓN] Solicitud de apertura de caja recibida. Usuario: '{}', Monto Inicial: ${}",
                currentUser.getUsername(), openingBalance);
        try {
            // 1. REGLA DE NEGOCIO: Verificar que no tenga un turno ya abierto
            // 🔥 CORRECCIÓN: Usamos findByUserIdAndStatus porque la entidad relaciona a un objeto 'User user'
            Optional<CashShift> activeShift = cashShiftRepository.findByUserIdAndStatus(
                    currentUser.getId(),
                    ShiftStatus.OPEN
            );
            if (activeShift.isPresent()) {
                log.warn("[NEGOCIO FALLIDO] El usuario '{}' intentó abrir caja, pero ya tiene el turno ID: [{}] en estado OPEN.",
                        currentUser.getUsername(), activeShift.get().getId());
                throw new IllegalStateException("El usuario ya tiene un turno de caja abierto.");
            }
            // 2. Crear la entidad del turno usando el Builder de Lombok
            CashShift newShift = CashShift.builder()
                    .user(currentUser)
                    .startTime(LocalDateTime.now())
                    .startingCash(openingBalance)
                    .status(ShiftStatus.OPEN) // Es bueno declararlo explícitamente aunque tengas un default
                    .build();
            // 3. Guardar en la base de datos
            newShift = cashShiftRepository.save(newShift);
            log.info("[ÉXITO] Turno de caja creado exitosamente. ID asignado: [{}], Cajero: '{}'",
                    newShift.getId(), currentUser.getUsername());
            return utilsPOS.mapToResponse(newShift);
        } catch (Exception e) {
            // Capturamos cualquier excepción para marcar la auditoría como fallida
            exito = false;
            mensajeError = e.getMessage();
            log.error("[ERROR CRÍTICO] Falló el proceso de apertura de caja para el usuario '{}'. Motivo: {}",
                    currentUser.getUsername(), e.getMessage());
            throw e; // Se relanza para provocar el Rollback de la transacción en JPA
        } finally {
            // Este bloque se ejecuta SIEMPRE, garantizando que el log de auditoría nunca se pierda
            long tiempoEjecucion = System.currentTimeMillis() - startTimeMillis;
            log.info("Finalizando ejecución de openShift. Tiempo: {}ms. Procediendo a registrar en tabla de auditoría.", tiempoEjecucion);
            auditLogService.logActivity(
                    "ABRIR_CAJA",
                    "OpenShiftService.openShift",
                    "Fondo Inicial Apertura: " + openingBalance,
                    startTimeMillis,
                    exito,
                    mensajeError,
                    request.getRemoteAddr(),
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getUserPrincipal().getName());
        }
    }
}
