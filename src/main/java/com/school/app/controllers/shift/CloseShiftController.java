package com.school.app.controllers.shift;

import com.school.app.dto.requets.CloseShiftRequest;
import com.school.app.dto.response.CashShiftResponse;
import com.school.app.dto.response.CloseCashShiftResponse;
import com.school.app.services.shift.CloseShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/pos/shifts") // Ajusta la ruta base según tu API
@RequiredArgsConstructor
public class CloseShiftController {

    // Asegúrate de que el nombre coincida con la clase donde tienes tu método closeShift
    private final CloseShiftService closeShiftService;

    /**
     * Endpoint para realizar el arqueo y cierre de caja.
     * POST /api/v1/cash-shifts/{shiftId}/close
     *
     * @param shiftId El identificador único del turno.
     * @param request El payload que contiene el dinero físico contado por el cajero.
     * @return Los datos del turno cerrado (CashShiftResponse).
     */
    @PostMapping("/{shiftId}/close")
    public ResponseEntity<CloseCashShiftResponse> closeShift(
            @PathVariable UUID shiftId,
            @Valid @RequestBody CloseShiftRequest request) {
        log.info("REST Request: Solicitud para cerrar el turno ID: {}. Efectivo declarado: {}",
                shiftId, request.declaredCash());
        // Delegamos la lógica pesada y transaccional a la capa de servicio
        CloseCashShiftResponse response = closeShiftService.closeShift(shiftId, request);
        log.info("REST Response: Turno ID {} cerrado exitosamente.", shiftId);
        return ResponseEntity.ok(response);
    }
}
