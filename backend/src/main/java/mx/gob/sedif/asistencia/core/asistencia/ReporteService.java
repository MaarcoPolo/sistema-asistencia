package mx.gob.sedif.asistencia.core.asistencia;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReporteService {

    public byte[] generarReporteExcel(List<AsistenciaReporteRecord> asistencias) throws IOException {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        // AGREGAMOS JUSTIFICACIÓN AL ENCABEZADO
        String[] headers = {
            "No. Control", "Nombre Completo", "Área", "Fecha", 
            "Hora Entrada", "Hora Salida", "Estatus", "Justificación", "IP de Registro"
        };

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Asistencias");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowNum = 1;
            for (AsistenciaReporteRecord record : asistencias) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(record.usuarioNumeroControl());
                row.createCell(1).setCellValue(record.usuarioNombreCompleto());
                row.createCell(2).setCellValue(record.areaNombre());
                row.createCell(3).setCellValue(record.fecha().toString());
                row.createCell(4).setCellValue(record.horaEntrada() != null ? record.horaEntrada().format(timeFormatter) : "---");
                row.createCell(5).setCellValue(record.horaSalida() != null ? record.horaSalida().format(timeFormatter) : "---");
                row.createCell(6).setCellValue(obtenerTextoIncidencia(record.estatusIncidencia()));
                // AGREGAMOS LA CELDA DE JUSTIFICACIÓN
                row.createCell(7).setCellValue(record.motivoJustificacion() != null ? record.motivoJustificacion() : "No");
                row.createCell(8).setCellValue(record.ipRegistro() != null ? record.ipRegistro() : "N/A");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generarReportePdf(List<AsistenciaReporteRecord> asistencias, String subtituloFiltros) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font fontTituloPrincipal = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, Color.BLACK);
            com.lowagie.text.Font fontSubtitulo = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.NORMAL, Color.BLACK);
            com.lowagie.text.Font fontSubtituloFiltros = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.ITALIC, Color.DARK_GRAY);
            
            Paragraph tituloPrincipal = new Paragraph("SISTEMA ESTATAL DIF", fontTituloPrincipal);
            tituloPrincipal.setAlignment(Element.ALIGN_CENTER);

            Paragraph subtitulo = new Paragraph("SISTEMA DE ASISTENCIA", fontSubtitulo);
            subtitulo.setAlignment(Element.ALIGN_CENTER);

            Paragraph subtituloFiltrosParaPdf = new Paragraph(subtituloFiltros, fontSubtituloFiltros);
            subtituloFiltrosParaPdf.setAlignment(Element.ALIGN_CENTER);

            document.add(tituloPrincipal);
            document.add(subtitulo);
            document.add(subtituloFiltrosParaPdf);
            document.add(new Paragraph(" "));

            // CAMBIAMOS A 9 COLUMNAS Y AJUSTAMOS LOS ANCHOS
            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1.8f, 3.2f, 3f, 1.8f, 1.8f, 1.8f, 1.6f, 2.2f, 2.2f });

            com.lowagie.text.Font fontHeader = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, Color.WHITE);
            // AGREGAMOS JUSTIFICACIÓN AL ENCABEZADO
            String[] headers = {"No. Control", "Nombre", "Área", "Fecha", "Entrada", "Salida", "Estatus", "Justificación", "IP Registro"};
            
            for (String headerTitle : headers) {
                PdfPCell header = new PdfPCell();
                header.setBackgroundColor(new Color(63, 81, 181));
                header.setBorderWidth(1);
                header.setHorizontalAlignment(Element.ALIGN_CENTER);
                header.setVerticalAlignment(Element.ALIGN_MIDDLE);
                header.setPaddingBottom(5);
                header.setPhrase(new Phrase(headerTitle, fontHeader));
                table.addCell(header);
            }

            com.lowagie.text.Font fontContenido = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.NORMAL, Color.BLACK);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            for (AsistenciaReporteRecord record : asistencias) {
                table.addCell(new Phrase(record.usuarioNumeroControl(), fontContenido));
                table.addCell(new Phrase(record.usuarioNombreCompleto(), fontContenido));
                table.addCell(new Phrase(record.areaNombre(), fontContenido));
                table.addCell(new Phrase(record.fecha().toString(), fontContenido));
                table.addCell(new Phrase(record.horaEntrada() != null ? record.horaEntrada().format(timeFormatter) : "---", fontContenido));
                table.addCell(new Phrase(record.horaSalida() != null ? record.horaSalida().format(timeFormatter) : "---", fontContenido));
                table.addCell(new Phrase(obtenerTextoIncidencia(record.estatusIncidencia()), fontContenido));
                // AGREGAMOS LA CELDA DE JUSTIFICACIÓN
                table.addCell(new Phrase(record.motivoJustificacion() != null ? record.motivoJustificacion() : "No", fontContenido));
                table.addCell(new Phrase(record.ipRegistro() != null ? record.ipRegistro() : "N/A", fontContenido));
            }

            document.add(table);
            document.close();

            return out.toByteArray();
        }
    }

    private String obtenerTextoIncidencia(Integer estatus) {
        if (estatus == null) return "Desconocido";
        return switch (estatus) {
            case 0 -> "OK";
            case 1 -> "Retardo";
            case 2 -> "Falta Total";
            case 3 -> "Omisión Entrada";
            case 4 -> "Omisión Salida";
            default -> "Desconocido";
        };
    }

    public byte[] generarReporteSancionesPdf(List<ResumenSancionesRecord> sanciones, String periodo) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate()); // Horizontal
            PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font fontTitulo = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD, Color.BLACK);
            com.lowagie.text.Font fontSubtitulo = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.ITALIC, Color.DARK_GRAY);
            
            Paragraph titulo = new Paragraph("SISTEMA ESTATAL DIF - REPORTE DE SANCIONES POR INCIDENCIAS", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            Paragraph subtitulo = new Paragraph("Periodo: " + periodo + " | (Solo incidencias no justificadas)", fontSubtitulo);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo); document.add(subtitulo); document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1.5f, 3f, 1.5f, 2f, 1.5f, 2f, 1.5f });

            com.lowagie.text.Font fontHeader = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, Color.WHITE);
            String[] headers = {"No. Control", "Nombre", "Retardos (Días)", "Fechas Retardos", "Faltas/Omisiones (Días)", "Fechas Faltas", "Total Descuento"};
            
            for (String headerTitle : headers) {
                PdfPCell header = new PdfPCell(new Phrase(headerTitle, fontHeader));
                header.setBackgroundColor(new Color(211, 47, 47)); // Rojo auditoría
                header.setHorizontalAlignment(Element.ALIGN_CENTER);
                header.setPaddingBottom(5);
                table.addCell(header);
            }

            com.lowagie.text.Font fontCont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.NORMAL, Color.BLACK);

            for (ResumenSancionesRecord r : sanciones) {
                table.addCell(new Phrase(r.numeroControl(), fontCont));
                table.addCell(new Phrase(r.nombreCompleto(), fontCont));
                table.addCell(new Phrase(r.totalRetardos() + " (" + r.diasDescuentoRetardos() + ")", fontCont));
                table.addCell(new Phrase(r.fechasRetardos().toString(), fontCont)); // Muestra [2026-05-01, 2026-05-04]
                table.addCell(new Phrase(r.totalFaltasYOmisiones() + " (" + r.diasDescuentoFaltas() + ")", fontCont));
                table.addCell(new Phrase(r.fechasFaltasYOmisiones().toString(), fontCont));
                
                PdfPCell celdaTotal = new PdfPCell(new Phrase(String.valueOf(r.totalDiasDescontar()), new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, Color.RED)));
                celdaTotal.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(celdaTotal);
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        }
    }

    public byte[] generarReporteSancionesExcel(List<ResumenSancionesRecord> sanciones, String periodo) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sanciones");

            // Encabezados detallados para Excel
            String[] headers = {
                "No. Control", "Nombre Completo", "Área", 
                "Retardos (Cant)", "Descuento Retardos (Días)", "Fechas Retardos", 
                "Faltas/Omisiones (Cant)", "Descuento Faltas (Días)", "Fechas Faltas", 
                "Total a Descontar (Días)"
            };

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowNum = 1;
            for (ResumenSancionesRecord r : sanciones) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.numeroControl());
                row.createCell(1).setCellValue(r.nombreCompleto());
                row.createCell(2).setCellValue(r.area());
                row.createCell(3).setCellValue(r.totalRetardos());
                row.createCell(4).setCellValue(r.diasDescuentoRetardos());
                row.createCell(5).setCellValue(r.fechasRetardos().toString());
                row.createCell(6).setCellValue(r.totalFaltasYOmisiones());
                row.createCell(7).setCellValue(r.diasDescuentoFaltas());
                row.createCell(8).setCellValue(r.fechasFaltasYOmisiones().toString());
                row.createCell(9).setCellValue(r.totalDiasDescontar());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}