package com.school.app.controllers.shift;

import com.school.app.dto.response.CashShiftResponse;
import com.school.app.entity.User;
import com.school.app.repository.UserRepository;
import com.school.app.services.shift.StatusCashShiftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pos/shifts")
public class ShiftController {

    private final StatusCashShiftService statusCashShiftService;
    private final UserRepository userRepository;

    /**
     * Endpoint: GET /api/pos/shifts/active
     * Propósito: Angular lo llama al arrancar el TerminalComponent para saber si muestra
     * el modal de "Abrir Caja" o la pantalla de cobro.
     */
    @PreAuthorize("hasAnyAuthority('SELLER', 'ADMIN')")
    @GetMapping("/active")
    public ResponseEntity<CashShiftResponse> getActiveShift(Authentication authentication) {
        // 1. Obtenemos el username del token JWT
        String username = authentication.getName();
        log.info("Verificando turno activo para el usuario: {}", username);
        // 2. Buscamos al usuario en la BD (podemos confiar que existe si el token es válido)
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado en BD"));
        // 3. Consultamos el servicio
        Optional<CashShiftResponse> activeShift = statusCashShiftService.getActiveShiftForUser(currentUser);
        // 4. Si hay turno, retornamos 200 OK con los datos.
        // Si no hay turno, retornamos 200 OK sin cuerpo (null en Angular), lo cual indica que debe abrir caja.
        return activeShift
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok().build());
    }
}
