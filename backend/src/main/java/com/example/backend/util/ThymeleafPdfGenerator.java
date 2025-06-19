package com.example.backend.util;

import com.lowagie.text.pdf.BaseFont;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.OutputStream;

public class ThymeleafPdfGenerator {
    
    private static final TemplateEngine templateEngine = new TemplateEngine();

    public static void generatePdf(String template, Context context, OutputStream outputStream) throws Exception {
        String html = templateEngine.process(template, context);
        
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(outputStream);
    }
}