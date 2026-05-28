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
import org.apache.poi.ss.usermodel.Font;
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
        String[] headers = {
            "No. Control", "Nombre", "Área", "Fecha", "Entrada", "Salida", "Estatus", "IP Registro"
        };

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Asistencias");

            // Estilo para el encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Crear fila de encabezados
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos
            int rowIdx = 1;
            for (AsistenciaReporteRecord record : asistencias) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(record.usuarioNumeroControl());
                row.createCell(1).setCellValue(record.usuarioNombreCompleto());
                row.createCell(2).setCellValue(record.areaNombre());
                row.createCell(3).setCellValue(record.fecha().toString());
                row.createCell(4).setCellValue(record.horaEntrada() != null ? record.horaEntrada().format(timeFormatter) : "---");
                row.createCell(5).setCellValue(record.horaSalida() != null ? record.horaSalida().format(timeFormatter) : "---");
                row.createCell(6).setCellValue(obtenerTextoIncidencia(record.estatusIncidencia()));
                row.createCell(7).setCellValue(record.ipRegistro() != null ? record.ipRegistro() : "N/A");
            }

            // Autoajustar columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generarReportePdf(List<AsistenciaReporteRecord> asistencias, String subtitulo) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate()); // Horizontal
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, Color.BLACK);
            Paragraph title = new Paragraph("Reporte de Asistencias", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);

            // Subtítulo (Filtros)
            com.lowagie.text.Font subtitleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, Color.DARK_GRAY);
            Paragraph subTitle = new Paragraph(subtitulo, subtitleFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            subTitle.setSpacingAfter(20);
            document.add(subTitle);

            // Tabla
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 3.5f, 2.5f, 1.5f, 1.5f, 1.5f, 1.5f, 2.0f});

            // Encabezados de la tabla
            String[] headers = {"No. Control", "Nombre", "Área", "Fecha", "Entrada", "Salida", "Estatus", "IP Registro"};
            com.lowagie.text.Font fontHeader = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD, Color.WHITE);
            
            for (String headerTitle : headers) {
                PdfPCell header = new PdfPCell();
                header.setBackgroundColor(Color.DARK_GRAY);
                header.setHorizontalAlignment(Element.ALIGN_CENTER);
                header.setVerticalAlignment(Element.ALIGN_MIDDLE);
                header.setPaddingBottom(5);
                header.setPhrase(new Phrase(headerTitle, fontHeader));
                table.addCell(header);
            }

            com.lowagie.text.Font fontContenido = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, Color.BLACK);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            for (AsistenciaReporteRecord record : asistencias) {
                table.addCell(new Phrase(record.usuarioNumeroControl(), fontContenido));
                table.addCell(new Phrase(record.usuarioNombreCompleto(), fontContenido));
                table.addCell(new Phrase(record.areaNombre(), fontContenido));
                table.addCell(new Phrase(record.fecha().toString(), fontContenido));
                table.addCell(new Phrase(record.horaEntrada() != null ? record.horaEntrada().format(timeFormatter) : "---", fontContenido));
                table.addCell(new Phrase(record.horaSalida() != null ? record.horaSalida().format(timeFormatter) : "---", fontContenido));
                table.addCell(new Phrase(obtenerTextoIncidencia(record.estatusIncidencia()), fontContenido));
                table.addCell(new Phrase(record.ipRegistro() != null ? record.ipRegistro() : "N/A", fontContenido));
            }

            document.add(table);
            document.close();
            
            return out.toByteArray();
        }
    }

    private String obtenerTextoIncidencia(Integer estatus) {
        return switch (estatus) {
            case 0 -> "OK";
            case 1 -> "Retardo";
            case 2 -> "Falta Total";
            case 3 -> "Omisión Entrada";
            case 4 -> "Omisión Salida";
            default -> "Desconocido";
        };
    }
}