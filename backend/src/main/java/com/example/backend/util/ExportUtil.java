package com.example.backend.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.backend.model.Produit;

public class ExportUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    public static class ProduitExcelExportUtils {
        private final XSSFWorkbook workbook;
        private XSSFSheet sheet;
        private final List<Produit> produitList;

        public ProduitExcelExportUtils(List<Produit> produitList) {
            this.produitList = produitList;
            this.workbook = new XSSFWorkbook();
        }

        private void createCell(Row row, int columnCount, Object value, CellStyle style) {
            sheet.autoSizeColumn(columnCount);
            Cell cell = row.createCell(columnCount);
            
            if (value == null) {
                cell.setCellValue("");
            } else if (value instanceof Integer) {
                cell.setCellValue((Integer) value);
            } else if (value instanceof Double) {
                cell.setCellValue((Double) value);
            } else if (value instanceof Boolean) {
                cell.setCellValue((Boolean) value);
            } else if (value instanceof Long) {
                cell.setCellValue((Long) value);
            } else if (value instanceof LocalDateTime) {
                LocalDateTime dateTime = (LocalDateTime) value;
                cell.setCellValue(dateTime.format(DATE_FORMATTER));
            } else {
                cell.setCellValue(value.toString());
            }
            
            cell.setCellStyle(style);
        }

        private void createHeaderRow() {
            sheet = workbook.createSheet("Informations Produits");
            Row row = sheet.createRow(0);
            CellStyle style = workbook.createCellStyle();
            XSSFFont font = workbook.createFont();
            font.setBold(true);
            font.setFontHeight(20);
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            createCell(row, 0, "Inventaire des Produits", style);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8)); // 9 colonnes (0-8)

            row = sheet.createRow(1);
            font = workbook.createFont(); // Nouvel objet font
            font.setBold(true);
            font.setFontHeight(16);
            
            CellStyle headerStyle = workbook.createCellStyle(); // Nouveau style
            headerStyle.setFont(font);
            
            createCell(row, 0, "ID", headerStyle);
            createCell(row, 1, "Nom", headerStyle);
            createCell(row, 2, "Code Barre", headerStyle);
            createCell(row, 3, "Référence", headerStyle);
            createCell(row, 4, "Description", headerStyle);
            createCell(row, 5, "Prix", headerStyle);
            createCell(row, 6, "Unité", headerStyle);
            createCell(row, 7, "Catégorie", headerStyle);
            createCell(row, 8, "Date", headerStyle);
        }

        private void writeProduitData() {
            int rowCount = 2;
            CellStyle style = workbook.createCellStyle();
            XSSFFont font = workbook.createFont();
            font.setFontHeight(14);
            style.setFont(font);

            for (Produit produit : produitList) {
                Row row = sheet.createRow(rowCount++);
                int columnCount = 0;
                createCell(row, columnCount++, produit.getId(), style);
                createCell(row, columnCount++, produit.getNom(), style);
                createCell(row, columnCount++, produit.getCodeBar(), style);
                createCell(row, columnCount++, produit.getReference(), style);
                createCell(row, columnCount++, produit.getDescription(), style);
                createCell(row, columnCount++, produit.getPrix(), style);
                createCell(row, columnCount++, produit.getUnite(), style);
                
                // Vérifier si la catégorie existe
                if (produit.getCategory() != null) {
                    createCell(row, columnCount++, produit.getCategory().getName(), style);
                } else {
                    createCell(row, columnCount++, "", style);
                }
                
                createCell(row, columnCount++, produit.getDatecremod(), style);
            }
        }

        public void exportDataToExcel(HttpServletResponse response) throws IOException {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=produits_inventaire.xlsx");
            
            createHeaderRow();
            writeProduitData();
            
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                workbook.close();
            }
        }
    }
    
    public static class ProduitCsvExportUtils {
        private final List<Produit> produitList;
        private static final String CSV_SEPARATOR = ";";
        
        public ProduitCsvExportUtils(List<Produit> produitList) {
            this.produitList = produitList;
        }
        
        private String escapeSpecialCharacters(String data) {
            if (data == null) {
                return "";
            }
            String escapedData = data.replaceAll("\\R", " ");
            if (data.contains(CSV_SEPARATOR) || data.contains("\"") || data.contains("'")) {
                data = data.replace("\"", "\"\"");
                escapedData = "\"" + data + "\"";
            }
            return escapedData;
        }
        
        private String[] getHeaders() {
            return new String[] {
                "ID", "Nom", "Code Barre", "Référence", "Description", "Prix", 
                "Unité", "Catégorie", "Date"
            };
        }
        
        private String[] getRowData(Produit produit) {
            String categoryName = "";
            if (produit.getCategory() != null) {
                categoryName = produit.getCategory().getName();
            }
            
            String dateStr = "";
            if (produit.getDatecremod() != null) {
                dateStr = produit.getDatecremod().format(DATE_FORMATTER);
            }
            
            return new String[] {
                produit.getId() != null ? produit.getId().toString() : "",
                escapeSpecialCharacters(produit.getNom()),
                escapeSpecialCharacters(produit.getCodeBar()),
                escapeSpecialCharacters(produit.getReference()),
                escapeSpecialCharacters(produit.getDescription()),
                String.valueOf(produit.getPrix()),
                escapeSpecialCharacters(produit.getUnite()),
                escapeSpecialCharacters(categoryName),
                dateStr
            };
        }
        
        public void exportDataToCsv(HttpServletResponse response) throws IOException {
            response.setContentType("text/csv");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=produits_inventaire.csv");
            
            try (PrintWriter writer = response.getWriter()) {
                // Écrire l'en-tête BOM UTF-8 pour une compatibilité Excel optimale
                writer.write('\ufeff');
                
                // Écrire l'en-tête
                writer.println(String.join(CSV_SEPARATOR, getHeaders()));
                
                // Écrire les données
                for (Produit produit : produitList) {
                    String[] rowData = getRowData(produit);
                    writer.println(String.join(CSV_SEPARATOR, rowData));
                }
            }
        }
    }
    
    public static class ProduitPdfExportUtils {
        private final List<Produit> produitList;
        
        public ProduitPdfExportUtils(List<Produit> produitList) {
            this.produitList = produitList;
        }
        
        public void exportDataToPdf(HttpServletResponse response) throws IOException, DocumentException {
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=produits_inventaire.pdf");
            
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, response.getOutputStream());
            
            document.open();
            
            // Ajouter le titre
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Paragraph title = new Paragraph("Inventaire des Produits", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Ajouter la date de génération
            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.DARK_GRAY);
            Paragraph dateGeneration = new Paragraph("Généré le: " + 
                LocalDateTime.now().format(DATE_FORMATTER), dateFont);
            dateGeneration.setAlignment(Element.ALIGN_RIGHT);
            dateGeneration.setSpacingAfter(20);
            document.add(dateGeneration);
            
            // Créer le tableau
            PdfPTable table = new PdfPTable(9); // 9 colonnes
            table.setWidthPercentage(100);
            
            // Définir les largeurs relatives des colonnes
            float[] columnWidths = {0.5f, 1.5f, 1.2f, 1.2f, 2.0f, 0.8f, 0.8f, 1.2f, 1.2f};
            table.setWidths(columnWidths);
            
            // Définir l'en-tête du tableau
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
            
            String[] headers = {"ID", "Nom", "Code Barre", "Référence", "Description", 
                "Prix", "Unité", "Catégorie", "Date"};
            
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(BaseColor.DARK_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(5);
                table.addCell(cell);
            }
            
            // Ajouter les données
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
            boolean alternateColor = false;
            
            for (Produit produit : produitList) {
                alternateColor = !alternateColor;
                BaseColor backgroundColor = alternateColor ? 
                    new BaseColor(240, 240, 240) : BaseColor.WHITE;
                
                addCell(table, produit.getId() != null ? produit.getId().toString() : "", 
                        dataFont, backgroundColor);
                addCell(table, produit.getNom(), dataFont, backgroundColor);
                addCell(table, produit.getCodeBar(), dataFont, backgroundColor);
                addCell(table, produit.getReference(), dataFont, backgroundColor);
                
                // Pour la description, on limite la longueur à 100 caractères
                String description = produit.getDescription();
                if (description != null && description.length() > 100) {
                    description = description.substring(0, 97) + "...";
                }
                addCell(table, description, dataFont, backgroundColor);
                
                addCell(table, String.format("%.2f", produit.getPrix()), dataFont, backgroundColor);
                addCell(table, produit.getUnite(), dataFont, backgroundColor);
                
                String category = "";
                if (produit.getCategory() != null) {
                    category = produit.getCategory().getName();
                }
                addCell(table, category, dataFont, backgroundColor);
                
                String date = "";
                if (produit.getDatecremod() != null) {
                    date = produit.getDatecremod().format(DATE_FORMATTER);
                }
                addCell(table, date, dataFont, backgroundColor);
            }
            
            document.add(table);
            
            // Ajouter des informations de pied de page
            Paragraph footer = new Paragraph("Nombre total de produits: " + produitList.size(), 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            footer.setSpacingBefore(20);
            document.add(footer);
            
            document.close();
        }
        
        private void addCell(PdfPTable table, String value, Font font, BaseColor backgroundColor) {
            PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", font));
            cell.setBackgroundColor(backgroundColor);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }
}