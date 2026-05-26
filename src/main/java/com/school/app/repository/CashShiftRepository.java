package com.school.app.repository;

import com.school.app.entity.CashShift;
import com.school.app.enums.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashShiftRepository extends JpaRepository<CashShift, UUID> {

    // Busca si el usuario actual ya tiene un turno abierto (Para evitar que abra dos al mismo tiempo)
    Optional<CashShift> findByUserIdAndStatus(UUID cashierId, ShiftStatus status);

    /**
     * Busca un turno por su ID exacto y su Estado actual.
     * Spring Data JPA traduce esto a:
     * SELECT * FROM shifts WHERE id = ? AND status = ?
     *
     * @param id El ID del turno
     * @param status El estado (ej. "OPEN", "CLOSED")
     * @return Un Optional que contiene el turno si lo encuentra, o vacío si no existe
     */
    Optional<CashShift> findByIdAndStatus(UUID id, ShiftStatus status);

    /**
     * Recupera una lista de todos los turnos de caja que coinciden con el estado especificado.
     * <p>
     * En la operativa del Punto de Venta, este método es fundamental para:
     * <ul>
     *   <li>Si se pasa {@code ShiftStatus.OPEN}: Obtener los turnos actualmente activos
     *       para calcular en tiempo real el efectivo físico esperado en los cajones.</li>
     *   <li>Si se pasa {@code ShiftStatus.CLOSED}: Obtener el historial de turnos
     *       finalizados para auditorías o reportes de cortes de caja (Reportes Z).</li>
     * </ul>
     * </p>
     *
     * @param status El estado del turno por el cual filtrar (ej. {@code ShiftStatus.OPEN}
     *               o {@code ShiftStatus.CLOSED}).
     * @return Una lista {@link List} que contiene los turnos coincidentes.
     *         Retorna una lista vacía si no se encuentra ningún turno con dicho estado.
     */
    List<CashShift> findAllByStatus(ShiftStatus status);
}
