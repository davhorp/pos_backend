package com.school.app.controllers.product;

import com.school.app.dto.response.ProductResponse;
import com.school.app.services.product.AllProductsServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pos/products")
@RequiredArgsConstructor
public class AllProductsController {

    private final AllProductsServices allProductsServices;

    /**
     * Obtiene el catálogo completo de productos activos para la terminal de Punto de Venta (POS).
     * Este endpoint es utilizado para alimentar la cuadrícula de acceso rápido y la búsqueda en memoria.
     *
     * @return ResponseEntity con una lista de objetos ProductResponse.
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getQuickAccessProducts() {
        log.info("[ProductController] Solicitud recibida para cargar el catálogo rápido de productos.");

        List<ProductResponse> products = allProductsServices.getAllActiveProducts();

        log.info("[ProductController] Retornando {} productos activos al POS.", products.size());
        return ResponseEntity.ok(products);
    }
}
