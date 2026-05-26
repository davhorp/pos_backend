package com.school.app.repository;

import com.school.app.entity.SaleTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SaleTicketRepository extends JpaRepository<SaleTicket, UUID> {

    /**
     * Busca el documento físico del ticket asociado a una venta específica.
     *
     * @param saleId El UUID de la entidad Sale
     * @return El SaleTicket envuelto en un Optional
     */
    Optional<SaleTicket> findBySaleId(UUID saleId);

    /**
     * Verifica si una venta ya tiene un ticket generado para evitar duplicados.
     */
    boolean existsBySaleId(UUID saleId);

}
