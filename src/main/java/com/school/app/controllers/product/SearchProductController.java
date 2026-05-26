package com.school.app.controllers.product;

import com.school.app.dto.response.SearchProductResponse;
import com.school.app.services.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pos/products")
@RequiredArgsConstructor
public class SearchProductController {

    private final ProductService productService;

    /**
     * Endpoint para buscar un producto mediante el escáner de código de barras.
     */
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<SearchProductResponse> getProductByBarcode(
            @PathVariable String barcode,
            Authentication authentication) {

        log.info("Petición REST recibida para escanear código de barras: {}", barcode);

        return productService.findProductByBarcode(barcode, authentication)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()); // 404 si no existe
    }
}
