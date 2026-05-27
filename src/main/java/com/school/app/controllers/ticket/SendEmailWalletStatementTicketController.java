package com.school.app.controllers.ticket;

import com.school.app.dto.requets.EmailRequest;
import com.school.app.dto.requets.WalletStatementDto;
import com.school.app.services.email.EmailService;
import com.school.app.services.pdf.PdfGeneratorService;
import com.school.app.services.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@RestController
@RequestMapping("/api/pos/wallets")
@RequiredArgsConstructor
public class SendEmailWalletStatementTicketController {

    private final WalletService walletService;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final PdfGeneratorService pdfGeneratorService;

    @PostMapping("/{phone}/statement/email")
    public ResponseEntity<String> emailWalletStatement(
            @PathVariable String phone,
            @RequestBody EmailRequest request) {
        log.info("[WalletController] Solicitando envío de estado de cuenta por email para cliente: {}", phone);
        try {
            // 1. Obtener la data (Wallet, Transacciones, Ventas)
            WalletStatementDto statementData = walletService.getStatementData(phone);
            // 2. Inyectar la data en la plantilla HTML de Thymeleaf
            Context context = new Context();
            context.setVariable("statement", statementData);
            String htmlContent = templateEngine.process("wallet-statement-template", context);
            // 3. Convertir el HTML a PDF (en memoria)
            log.debug("[WalletController] Generando PDF para cliente: {}", phone);
            byte[] pdfBytes = pdfGeneratorService.generatePdfFromHtml(htmlContent);
            // 4. Enviar el correo con el adjunto
            String fileName = "Estado_Cuenta_" + phone + ".pdf";
            emailService.sendEmailWithAttachment(
                    request.email(),
                    "Estado de Cuenta Monedero Davho's",
                    "Hola, adjunto encontrarás el detalle de tus movimientos.",
                    pdfBytes,
                    fileName
            );
            log.info("[WalletController] Estado de cuenta enviado exitosamente a: {}", request.email());
            return ResponseEntity.ok("Estado de cuenta enviado exitosamente al correo del cliente.");
        } catch (IllegalArgumentException e) {
            log.warn("[WalletController] Error de negocio: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("[WalletController] Error crítico al procesar email del estado de cuenta para {}: {}", phone, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error al enviar el estado de cuenta. Intente más tarde.");
        }
    }
}
