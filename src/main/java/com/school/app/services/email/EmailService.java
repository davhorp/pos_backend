package com.school.app.services.email;

import com.school.app.audit.Auditable;
import com.school.app.entity.SystemAuditLog;
import com.school.app.exceptions.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Envía un correo electrónico con un archivo adjunto generado en memoria.
     *
     * @param to             Dirección de correo del destinatario.
     * @param subject        Asunto del correo.
     * @param text           Cuerpo del mensaje (texto plano o HTML).
     * @param attachment     Archivo a adjuntar en formato de arreglo de bytes.
     * @param attachmentName Nombre del archivo adjunto (ej. "documento.pdf").
     */
    @Auditable(action = SystemAuditLog.AuditAction.ENVIO_CORREO, entityName = "EMAIL") // Ajusta el Enum según tu configuración
    public void sendEmailWithAttachment(String to, String subject, String text, byte[] attachment, String attachmentName) {
        log.info("[EmailService] Preparando envío de correo a: {} | Asunto: {}", to, subject);
        long startTime = System.currentTimeMillis();
        try {
            // 1. Crear el mensaje MIME (permite HTML y adjuntos)
            MimeMessage message = mailSender.createMimeMessage();
            // El 'true' indica que el mensaje será multipart (es decir, tendrá adjuntos)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            // 2. Configurar cabeceras y cuerpo
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false); // Pon 'true' si el parámetro 'text' contiene código HTML
            // 3. Adjuntar el archivo desde memoria
            log.info("[EmailService] Adjuntando archivo: {} (Tamaño: {} bytes)", attachmentName, attachment.length);
            ByteArrayResource pdfResource = new ByteArrayResource(attachment);
            helper.addAttachment(attachmentName, pdfResource);
            // 4. Enviar correo
            mailSender.send(message);
            long duration = System.currentTimeMillis() - startTime;
            log.info("[EmailService] Correo enviado exitosamente a: {} | Archivo: {} | Tiempo: {}ms",
                    to, attachmentName, duration);
        } catch (MessagingException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[EmailService] Error de mensajería (MIME) al intentar enviar correo a: {}. Tiempo: {}ms. Excepción: {}",
                    to, duration, e.getMessage(), e);
            throw new RuntimeException("Error al estructurar el correo con archivo adjunto.", e);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[EmailService] Error CRÍTICO (Red/SMTP) al enviar correo a: {}. Tiempo: {}ms. Excepción: {}",
                    to, duration, e.getMessage(), e);
            throw new RuntimeException("Error de conexión al servidor de correo electrónico.", e);
        }
    }
}
