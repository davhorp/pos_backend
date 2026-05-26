package com.school.app.services.sale;

import com.school.app.audit.Auditable;
import com.school.app.dto.requets.SaleItemRequest;
import com.school.app.dto.requets.SaleRequest;
import com.school.app.dto.response.PaymentMethodResponse;
import com.school.app.dto.response.SaleResponse;
import com.school.app.entity.*;
import com.school.app.enums.ShiftStatus;
import com.school.app.repository.CashShiftRepository;
import com.school.app.repository.ProductRepository;
import com.school.app.repository.SaleRepository;
import com.school.app.services.auth.AuditLogService;
import com.school.app.services.wallet.WalletService;
import com.school.app.utils.UtilsPOS;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio encargado de la lógica de negocio para las ventas en el POS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaleService {

    private final UtilsPOS utilsPOS;
    private final WalletService walletService;
    private final SaleRepository saleRepository;
    private final AuditLogService auditLogService;
    private final ProductRepository productRepository;
    private final CashShiftRepository cashShiftRepository;

    /**
     * Procesa una venta completa. Descuenta inventario, asocia la venta al turno activo
     * y la guarda en la base de datos de forma transaccional.
     *
     * @param request Datos de la venta (productos y montos).
     * @param currentUser El cajero que está realizando el cobro.
     * @param paymentMethod El método de pago validado previamente por el controlador.
     * @return SaleResponse con el ID de la venta procesada.
     */
    @Auditable(action = SystemAuditLog.AuditAction.REGISTRO_VENTA_SISTEMA, entityName = "SALE")
    @Transactional // ¡CRÍTICO! Si algo falla, se revierte todo (nada se guarda en BD).
    public SaleResponse processCheckout(SaleRequest request, User currentUser, Sale.PaymentMethod paymentMethod) {
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        long startTimeMillis = System.currentTimeMillis();
        boolean exito = true;
        String mensajeError = null;
        UUID generatedSaleId = null; // Para guardarlo en la auditoría si es exitoso
        try {
            // 1. Validar que el usuario tenga un turno ABIERTO
            CashShift activeShift = cashShiftRepository.findByUserIdAndStatus(currentUser.getId(), ShiftStatus.OPEN)
                    .orElseThrow(() -> new IllegalStateException("No se puede cobrar: Debes tener un turno de caja abierto."));
            BigDecimal total = request.totalAmount();
            BigDecimal amountTendered = request.amountTendered() != null ? request.amountTendered() : total;
            BigDecimal changeAmount = BigDecimal.ZERO;
            String cardBrand = null;
            String lastFourDigits = null;
            String authCode = null;
            // 2. 🔀 BIFURCACIÓN DE LÓGICA SEGÚN EL MÉTODO DE PAGO
            switch (paymentMethod) {
                case CASH:
                    if (amountTendered.compareTo(total) < 0) {
                        throw new IllegalArgumentException("El monto recibido ($" + amountTendered + ") es menor al total de la venta ($" + total + ").");
                    }
                    changeAmount = amountTendered.subtract(total);
                    activeShift.setCashSales(activeShift.getCashSales().add(total));
                    break;
                case CREDIT_CARD:
                case DEBIT_CARD:
                    amountTendered = total;
                    cardBrand = request.cardBrand();
                    lastFourDigits = request.lastFourDigits();
                    authCode = request.authCode();
                    activeShift.setCardSales(activeShift.getCardSales().add(total));
                    break;
                case TRANSFER:
                    amountTendered = total;
                    cardBrand = request.cardBrand();
                    authCode = request.authCode();
                    activeShift.setTransferSales(activeShift.getTransferSales().add(total));
                    break;
                case QR:
                    amountTendered = total;
                    cardBrand = request.cardBrand();
                    authCode = request.authCode();
                    activeShift.setQrSales(activeShift.getQrSales().add(total));
                    break;
                default:
                    amountTendered = total;
                    break;
            }
            cashShiftRepository.save(activeShift);
            // 3. Construir la cabecera de la Venta (El Ticket Principal)
            Sale newSale = Sale.builder()
                    .user(currentUser)
                    .cashShift(activeShift)
                    .paymentMethod(paymentMethod)
                    .totalAmount(total)
                    .saleDate(LocalDateTime.now())
                    .amountTendered(amountTendered)
                    .changeAmount(changeAmount)
                    .cardBrand(cardBrand)
                    .lastFourDigits(lastFourDigits)
                    .authCode(authCode)
                    .ticketNumber(utilsPOS.generatePureNumericUUID())
                    .items(new ArrayList<>())
                    .build();
            log.debug("Procesando {} artículos para la venta. Método: {}", request.items().size(), paymentMethod);
            // 4. Procesar los productos del carrito (SaleItems)
            for (SaleItemRequest itemReq : request.items()) {
                Product product = productRepository.findById(itemReq.productId())
                        .orElseThrow(() -> new IllegalArgumentException("El producto con ID " + itemReq.productId() + " ya no existe."));
                if (product.getStockQuantity() < itemReq.quantity()) {
                    throw new IllegalStateException("Stock insuficiente. Producto: " + product.getName() +
                            " | Solicitado: " + itemReq.quantity() +
                            " | Disponible: " + product.getStockQuantity());
                }
                product.setStockQuantity(product.getStockQuantity() - itemReq.quantity());
                productRepository.save(product);
                BigDecimal quantity = BigDecimal.valueOf(itemReq.quantity());
                BigDecimal subtotal = itemReq.unitPrice().multiply(quantity);
                SaleItem saleItem = SaleItem.builder()
                        .sale(newSale)
                        .product(product)
                        .quantity(itemReq.quantity())
                        .unitPrice(itemReq.unitPrice())
                        .subtotal(subtotal)
                        .build();
                newSale.getItems().add(saleItem);
            }
            // 5. Guardar Venta en Cascada
            Sale savedSale = saleRepository.save(newSale);
            // ADICIÓN: Si el cliente proporciona su teléfono en la compra, se dispara el abono
            if (request.customerPhone() != null && !request.customerPhone().isBlank()) {
                walletService.accumulateBalance(
                        request.customerPhone(),
                        savedSale.getTotalAmount(),
                        savedSale.getTransactionId() // Tu formato "TX-XXXXXXXXX" o Folio numérico
                );
            }
            generatedSaleId = savedSale.getId(); // Rescatamos el ID para la auditoría
            log.info("Venta guardada en Base de Datos. Ticket ID: {}", generatedSaleId);
            // 6. Retornar el Ticket ID a Angular
            return new SaleResponse(
                    generatedSaleId,
                    "Venta procesada exitosamente."
            );
        } catch (Exception e) {
            exito = false;
            mensajeError = e.getMessage();
            log.error("[ERROR CRÍTICO] Falló el procesamiento de la venta. Motivo: {}", mensajeError);
            throw e; // Lanza la excepción para ejecutar el rollback del @Transactional
        } finally {
            // 7. 🔥 AUDITORÍA ASEGURADA (Se ejecuta falle o no falle el proceso)
            String detallesAuditoria = generatedSaleId != null
                    ? "Venta procesada exitosamente. Ticket ID: " + generatedSaleId + " | Total: $" + request.totalAmount() + " | Método: " + paymentMethod
                    : "Intento de venta fallido por $" + request.totalAmount() + " | Método: " + paymentMethod;
            auditLogService.logActivity(
                    "PROCESAR_VENTA",               // Acción adaptada al contexto actual
                    "SaleService.processCheckout",  // Clase y método real
                    detallesAuditoria,              // Detalles dinámicos
                    startTimeMillis,
                    exito,
                    mensajeError,
                    httpServletRequest.getRemoteAddr(),
                    httpServletRequest.getMethod(),
                    httpServletRequest.getRequestURI(),
                    httpServletRequest.getUserPrincipal().getName());
        }
    }

}
