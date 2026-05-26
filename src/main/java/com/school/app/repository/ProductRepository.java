package com.school.app.repository;

import com.school.app.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Busca un producto en la base de datos utilizando su código de barras físico.
     * Spring Data JPA genera automáticamente la consulta SQL (SELECT * FROM products WHERE barcode = ?)
     *
     * @param barcode El código de barras escaneado (ej. "75010111").
     * @return Un Optional con la entidad Product si existe, o vacío si no se encontró.
     */
    Optional<Product> findByBarcode(String barcode);

    /**
     * Opcional: Muy útil para validaciones a la hora de crear un producto nuevo
     * y evitar que dos productos tengan el mismo código de barras.
     */
    boolean existsByBarcode(String barcode);

}
