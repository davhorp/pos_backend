package com.school.app.services.pdf;

import com.lowagie.text.DocumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Service
public class PdfGeneratorService {

    /**
     * Convierte una cadena de texto HTML en un arreglo de bytes (PDF).
     *
     * @param htmlContent El código HTML generado por Thymeleaf.
     * @return El archivo PDF en formato byte[].
     */
    public byte[] generatePdfFromHtml(String htmlContent) {
        log.info("Iniciando conversión de HTML a PDF en memoria.");
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            // Asignamos el HTML al motor de renderizado
            renderer.setDocumentFromString(htmlContent);
            // Calculamos el diseño (CSS, posiciones, tamaños)
            renderer.layout();
            // Generamos el PDF y lo volcamos en el OutputStream
            renderer.createPDF(outputStream);
            log.info("PDF generado exitosamente. Tamaño: {} bytes", outputStream.size());
            return outputStream.toByteArray();
        } catch (DocumentException e) {
            log.error("Error estructural en la creación del documento PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al estructurar el PDF.", e);
        } catch (IOException e) {
            log.error("Error de I/O al generar el PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error de entrada/salida al procesar el PDF.", e);
        }
    }
}
