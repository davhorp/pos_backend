package com.school.app.controllers.shift;

import com.school.app.dto.requets.OpenShiftRequest;
import com.school.app.dto.response.CashShiftResponse;
import com.school.app.entity.User;
import com.school.app.repository.UserRepository;
import com.school.app.services.shift.OpenShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pos/shifts")
public class OpenShiftController {

    private final OpenShiftService openShiftService;
    private final UserRepository userRepository;

    /**
     * Endpoint: POST /api/pos/shifts/open
     * Propósito: Recibe el fondo inicial e inicia un nuevo turno.
     */
    @PreAuthorize("hasAnyAuthority('SELLER', 'ADMIN')")
    @PostMapping("/open")
    public ResponseEntity<CashShiftResponse> openShift(
            Authentication authentication,
            @Valid @RequestBody OpenShiftRequest request) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado en BD"));
        // Llamamos al servicio pasando el usuario y el dinero de apertura
        CashShiftResponse response = openShiftService.openShift(currentUser, request.openingBalance());
        // Devolvemos 201 CREATED
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
