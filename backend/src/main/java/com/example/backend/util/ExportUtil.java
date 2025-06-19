package com.example.backend.util;

import com.example.backend.model.Ecart;
import com.example.backend.model.PlanInventaire;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import jakarta.servlet.http.HttpServletResponse;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.util.List;

public class ExportUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static class EcartPdfExportUtils {
        private final List<Ecart> ecarts;
        private final PlanInventaire plan;

        public EcartPdfExportUtils(List<Ecart> ecarts, PlanInventaire plan) {
            this.ecarts = ecarts;
            this.plan = plan;
        }

        public void exportDataToPdf(HttpServletResponse response) throws DocumentException, IOException {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, response.getOutputStream());

            document.open();
            addDocumentHeader(document);
            addPlanInfo(document);
            addDataTable(document);
            addSummary(document);
            document.close();
        }

        private void addDocumentHeader(Document document) throws DocumentException {
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Rapport des Écarts d'Inventaire", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
        }

        private void addPlanInfo(Document document) throws DocumentException {
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.DARK_GRAY);
            Paragraph planInfo = new Paragraph();
            planInfo.add(new Chunk("Plan: " + plan.getNom() + "\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            planInfo.add("Date de début: " + plan.getDateDebut().format(DATE_FORMATTER) + "\n");
            planInfo.add("Date de fin: " + plan.getDateFin().format(DATE_FORMATTER) + "\n");
            planInfo.setSpacingAfter(20);
            document.add(planInfo);
        }

        private void addDataTable(Document document) throws DocumentException {
            PdfPTable table = createTable();
            addTableHeaders(table);
            addTableData(table);
            document.add(table);
        }

        private PdfPTable createTable() throws DocumentException {
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            float[] columnWidths = {3f, 2f, 2f, 1.5f, 1.5f, 1.5f, 2.5f, 2f};
            table.setWidths(columnWidths);
            return table;
        }

        private void addTableHeaders(PdfPTable table) {
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
            BaseColor headerBg = new BaseColor(63, 81, 181);

            String[] headers = {"Produit", "Code Barre", "Zone", "Théorique", "Compté", "Écart", "Date Validation", "Statut"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(5);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }
        }

        private void addTableData(PdfPTable table) {
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            BaseColor altRowColor = new BaseColor(240, 244, 255);

            for (int i = 0; i < ecarts.size(); i++) {
                Ecart ecart = ecarts.get(i);
                BaseColor rowColor = (i % 2 == 0) ? BaseColor.WHITE : altRowColor;
                
                addCell(table, ecart.getProduit().getNom(), rowColor, dataFont);
                addCell(table, ecart.getProduit().getCodeBarre(), rowColor, dataFont);
                addCell(table, ecart.getProduit().getZones().stream()
                        .map(z -> z.getName())
                        .findFirst()
                        .orElse("-"), rowColor, dataFont);
                addCell(table, String.valueOf(ecart.getQuantiteTheorique()), rowColor, dataFont);
                addCell(table, String.valueOf(ecart.getQuantiteComptee()), rowColor, dataFont);
                addCell(table, String.valueOf(ecart.getQuantiteComptee() - ecart.getQuantiteTheorique()), rowColor, dataFont);
                addCell(table, ecart.getDateValidation() != null ?
                        ecart.getDateValidation().format(DATE_FORMATTER) : "-", rowColor, dataFont);
                addCell(table, ecart.getStatut().toString(), rowColor, dataFont);
            }
        }

        private void addSummary(Document document) throws DocumentException {
            Paragraph summary = new Paragraph();
            summary.add(new Chunk("\nTotal des écarts: " + ecarts.size(), FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            summary.setSpacingBefore(20);
            document.add(summary);
        }

        private void addCell(PdfPTable table, String text, BaseColor backgroundColor, com.itextpdf.text.Font font) {
            PdfPCell cell = new PdfPCell(new Phrase(text, font));
            cell.setBackgroundColor(backgroundColor);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }

    public static class EcartExcelExportUtils {
        private final List<Ecart> ecarts;
        private final PlanInventaire plan;
        private final XSSFWorkbook workbook;

        public EcartExcelExportUtils(List<Ecart> ecarts, PlanInventaire plan) {
            this.ecarts = ecarts;
            this.plan = plan;
            this.workbook = new XSSFWorkbook();
        }

        public void exportDataToExcel(HttpServletResponse response) throws IOException {
            Sheet sheet = workbook.createSheet("Écarts d'inventaire");

            // Add plan info
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("Plan: " + plan.getNom());
            Row dateRow = sheet.createRow(1);
            dateRow.createCell(0).setCellValue("Période: " + 
                plan.getDateDebut().format(DATE_FORMATTER) + " - " + 
                plan.getDateFin().format(DATE_FORMATTER));

            Row headerRow = sheet.createRow(3);
            String[] headers = {
                "Produit", "Code Barre", "Zone", "Qté Théorique",
                "Qté Comptée", "Écart", "Date Validation", "Statut"
            };

            CellStyle headerStyle = createHeaderStyle();
            CellStyle dataStyle = createDataStyle();

            // Write headers
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Write data
            int rowNum = 4;
            for (Ecart ecart : ecarts) {
                Row row = sheet.createRow(rowNum++);
                addDataRow(row, ecart, dataStyle);
            }

            // Autosize columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
            workbook.close();
        }

        private void addDataRow(Row row, Ecart ecart, CellStyle style) {
            createCell(row, 0, ecart.getProduit().getNom(), style);
            createCell(row, 1, ecart.getProduit().getCodeBarre(), style);
            createCell(row, 2, ecart.getProduit().getZones().stream()
                    .map(z -> z.getName())
                    .findFirst()
                    .orElse("-"), style);
            createCell(row, 3, String.valueOf(ecart.getQuantiteTheorique()), style);
            createCell(row, 4, String.valueOf(ecart.getQuantiteComptee()), style);
            createCell(row, 5, String.valueOf(ecart.getQuantiteComptee() - ecart.getQuantiteTheorique()), style);
            createCell(row, 6, ecart.getDateValidation() != null ?
                    ecart.getDateValidation().format(DATE_FORMATTER) : "-", style);
            createCell(row, 7, ecart.getStatut().toString(), style);
        }

        private void createCell(Row row, int column, String value, CellStyle style) {
            Cell cell = row.createCell(column);
            cell.setCellValue(value);
            cell.setCellStyle(style);
        }

        private CellStyle createHeaderStyle() {
            CellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);

            XSSFFont font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            return style;
        }

        private CellStyle createDataStyle() {
            CellStyle style = workbook.createCellStyle();
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setAlignment(HorizontalAlignment.LEFT);
            return style;
        }
    }
}