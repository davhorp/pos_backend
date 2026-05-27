package com.school.app.services.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.app.audit.Auditable;
import com.school.app.dto.response.ProductResponse;
import com.school.app.entity.Product;
import com.school.app.entity.SystemAuditLog;
import com.school.app.repository.ProductRepository;
import com.school.app.services.auth.AuditLogService;
import com.school.app.utils.UtilsPOS;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllProductsServices {

    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;
    private final UtilsPOS utilsPOS;
    private final ObjectMapper objectMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Recupera todos los productos que están activos en el sistema y los mapea a su respectivo DTO.
     * * @return Lista de productos activos.
     */
    @Auditable(action = SystemAuditLog.AuditAction.ALL_PRODUCTS, entityName = "PRODUCT")
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllActiveProducts() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        log.info("[ProductService] Consultando base de datos para recuperar productos activos...");
        // Variables de estado para el bloque finally
        long startTime = System.currentTimeMillis();
        boolean exito = false;
        String mensajeError = null;
        String jsonPayload = "{}";
        try {
            List<Product> activeProducts = productRepository.findByIsActiveTrue();
            List<ProductResponse> responseList = activeProducts.stream()
                    .map(utilsPOS::mapToResponseProduct)
                    .collect(Collectors.toList());
            // Construimos un JSON dinámico con información relevante para la auditoría
            jsonPayload = objectMapper.writeValueAsString(Map.of(
                    "total_productos_enviados", responseList.size(),
                    "origen", "Carga inicial POS"
            ));
            exito = true;
            log.info("[ProductService] Consulta exitosa. {} productos mapeados.", responseList.size());
            return responseList;
        } catch (Exception e) {
            mensajeError = e.getMessage();
            jsonPayload = "{\"error\": \"Fallo al recuperar catálogo completo\"}";
            log.error("[ProductService] Error fatal al intentar recuperar el catálogo. Causa: {}", mensajeError, e);
            throw new RuntimeException("Error interno al cargar catálogo de productos", e);
        } finally {
            // Registro de auditoría tal como lo solicitaste
            auditLogService.logActivity(
                    "PRODUCTOS_ACTIVOS",                      // Acción adaptada
                    "ProductService.getAllActiveProducts",  // Módulo exacto ajustado al método actual
                    jsonPayload,                            // Detalles dinámicos en JSON
                    startTime,
                    exito,
                    mensajeError,
                    request.getRemoteAddr(),
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getUserPrincipal().getName()                                // Usuario extraído de forma segura
            );
        }
    }

}
