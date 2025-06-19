package com.example.backend.service;

import com.example.backend.model.PlanInventaire;
import com.example.backend.model.Produit;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfExportService {
    
    public byte[] generateProduitsPdf(List<Produit> produits) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Add title
        Paragraph title = new Paragraph("Liste des Produits")
            .setFontSize(18)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        // Create table
        float[] columnWidths = {3, 2, 2, 2, 1, 2, 2};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));

        // Add headers
        String[] headers = {"Nom", "Code Barre", "Référence", "Prix", "Qté", "Catégorie", "Sous-catégorie"};
        for (String header : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(header).setBold()));
        }

        // Add data
        for (Produit produit : produits) {
            table.addCell(new Cell().add(new Paragraph(produit.getNom())));
            table.addCell(new Cell().add(new Paragraph(produit.getCodeBarre())));
            table.addCell(new Cell().add(new Paragraph(produit.getReference())));
            table.addCell(new Cell().add(new Paragraph(String.format("%.2f €", produit.getPrix()))));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(produit.getQuantitetheo()))));
            table.addCell(new Cell().add(new Paragraph(produit.getCategory() != null ? produit.getCategory().getName() : "")));
            table.addCell(new Cell().add(new Paragraph(produit.getSubCategory() != null ? produit.getSubCategory().getName() : "")));
        }

        document.add(table);
        document.close();

        return baos.toByteArray();
    }


}