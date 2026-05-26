package com.school.app.repository;

import com.school.app.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    /**
     * Agrupa y suma el monto total de las ventas filtrando por el ID del turno y el método de pago.
     * Es ultra eficiente porque trae todos los desgloses en una sola consulta.
     *
     * @param shiftId El UUID del turno activo (CashShift).
     * @return Una lista de arreglos donde [0] es el PaymentMethod y [1] es el BigDecimal del total.
     */
    @Query("SELECT s.paymentMethod, SUM(s.totalAmount) FROM Sale s WHERE s.cashShift.id = :shiftId GROUP BY s.paymentMethod")
    List<Object[]> sumSalesByPaymentMethodAndShift(@Param("shiftId") UUID shiftId);

    /**
     * Calcula la suma total de los ingresos generados por todas las ventas
     * dentro de un rango de fechas y horas específico.
     * <p>
     * La consulta utiliza {@code COALESCE} para garantizar la seguridad de tipos.
     * Si no se registran ventas en el periodo consultado, la base de datos
     * devolverá de manera segura {@code 0} en lugar de {@code null}, previniendo
     * posibles excepciones de tipo {@link NullPointerException} en la capa de servicio.
     * </p>
     *
     * @param startDate La fecha y hora de inicio del periodo a consultar (inclusivo).
     * @param endDate   La fecha y hora de fin del periodo a consultar (exclusivo).
     * @return El monto total acumulado de las ventas en el periodo indicado.
     *         Retorna {@code BigDecimal.ZERO} si no hubo transacciones.
     */
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.createdAt >= :startDate AND s.createdAt < :endDate")
    BigDecimal sumTotalSalesBetweenDates(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Cuenta el número total de tickets (transacciones de venta) emitidos
     * dentro de un rango de fechas y horas específico.
     * <p>
     * Este método es útil para calcular métricas de flujo de clientes
     * y, combinado con los ingresos totales, permite determinar el ticket promedio
     * de un periodo determinado.
     * </p>
     *
     * @param startDate La fecha y hora de inicio del periodo a consultar (inclusivo).
     * @param endDate   La fecha y hora de fin del periodo a consultar (exclusivo).
     * @return La cantidad exacta de ventas registradas en el periodo.
     *         Retorna {@code 0} si no hubo transacciones.
     */
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.createdAt >= :startDate AND s.createdAt < :endDate")
    long countSalesBetweenDates(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Agrupa y suma el total de ventas por método de pago dentro de un rango de fechas.
     * Devuelve una lista de arreglos de objetos donde [0] es el método y [1] es la suma.
     */
    @Query("SELECT s.paymentMethod, COALESCE(SUM(s.totalAmount), 0) " +
            "FROM Sale s " +
            "WHERE s.createdAt >= :startDate AND s.createdAt < :endDate " +
            "GROUP BY s.paymentMethod")
    List<Object[]> sumTotalSalesByPaymentMethodBetweenDates(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

}
