package com.school.app.services.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.app.audit.Auditable;
import com.school.app.dto.requets.ProductResponse;
import com.school.app.entity.SystemAuditLog;
import com.school.app.entity.User;
import com.school.app.repository.ProductRepository;
import com.school.app.repository.SystemAuditLogRepository;
import com.school.app.repository.UserRepository;
import com.school.app.services.auth.AuditLogService;
import com.school.app.utils.UtilsPOS;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UtilsPOS utilsPOS;
    private final UserRepository userRepository;

    // Inyecciones para la auditoría manual
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    /**
     * Busca un producto por su código de barras físico.
     * <p>
     * Este método es invocado por la Terminal POS cada vez que el cajero utiliza
     * el escáner láser. Valida la existencia del producto y registra el evento
     * para auditorías de seguridad e inventario.
     * </p>
     *
     * @param barcode     El código de barras extraído por el hardware.
     * @param authentication El usuario (Cajero) que está operando la terminal.
     * @return Un {@link Optional} que contiene el DTO {@link ProductResponse} si el producto
     *         existe en el catálogo, o un Optional vacío si no se encuentra.
     */
    @Auditable(action = SystemAuditLog.AuditAction.SEARCH_PRODUCT, entityName = "PRODUCT")
    public Optional<ProductResponse> findProductByBarcode(String barcode, Authentication authentication) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        long startTime = System.currentTimeMillis();
        boolean exito = true;
        String mensajeError = null;
        Optional<ProductResponse> productOpt = Optional.empty();
        try {
            log.debug("Verificando sesión para el usuario: {}", authentication.getName());
            // 1. Buscamos al usuario en la BD
            User currentUser = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado en BD"));
            log.info("Usuario ID: {} inició escaneo de código de barras: {}", currentUser.getId(), barcode);
            // 2. Ejecución de la lógica de negocio (Búsqueda en BD)
            productOpt = productRepository.findByBarcode(barcode)
                    .map(utilsPOS::mapToProductResponse);
            // 3. Log detallado del resultado
            if (productOpt.isPresent()) {
                log.info("Producto encontrado exitosamente. ID: {}, Nombre: {}",
                        productOpt.get().id(), productOpt.get().name());
            } else {
                log.warn("Alerta de Inventario: El código de barras {} no está registrado en el sistema.", barcode);
            }
            return productOpt;
        } catch (Exception e) {
            exito = false;
            mensajeError = e.getMessage();
            log.error("Error inesperado al buscar producto por código de barras {}: {}", barcode, mensajeError);
            throw e;
        } finally {
            // 4. REGISTRO DE ENTIDAD DE AUDITORÍA (Garantizado
            // Armar el payload JSON con detalles específicos de esta búsqueda
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("codigo_barras_buscado", barcode);
            payloadMap.put("producto_encontrado", productOpt.isPresent());
            // Si se encontró, guardamos el nombre para que el log sea más legible
            productOpt.ifPresent(p -> payloadMap.put("producto_nombre", p.name()));
            String jsonPayload;
            try {
                jsonPayload = objectMapper.writeValueAsString(payloadMap);
            } catch (JsonProcessingException e) {
                log.error("Error al serializar payload de auditoría para producto", e);
                jsonPayload = "{ \"codigo_barras\": \"" + barcode + "\", \"error_json\": true }";
            }
            // Llamamos al método de 6 parámetros (el AuditLogService ya se encarga de la IP y la URL internamente)
            auditLogService.logActivity(
                    "BUSCAR_PRODUCTO",                      // Acción adaptada
                    "ProductService.findProductByBarcode",  // Módulo exacto
                    jsonPayload,                            // Detalles dinámicos en JSON
                    startTime,
                    exito,
                    mensajeError,
                    request.getRemoteAddr(),
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getUserPrincipal().getName());
        }
    }
}
