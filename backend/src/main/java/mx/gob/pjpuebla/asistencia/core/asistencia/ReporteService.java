package mx.gob.pjpuebla.asistencia.core.asistencia;

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
            "Matrícula", "Nombre Completo", "Área", "Fecha", 
            "Hora Entrada", "Hora Salida", "Retardo"
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
                row.createCell(0).setCellValue(record.usuarioMatricula());
                row.createCell(1).setCellValue(record.usuarioNombreCompleto());
                row.createCell(2).setCellValue(record.areaNombre());
                row.createCell(3).setCellValue(record.fecha().toString());
                row.createCell(4).setCellValue(record.horaEntrada() != null ? record.horaEntrada().format(timeFormatter) : "---");
                row.createCell(5).setCellValue(record.horaSalida() != null ? record.horaSalida().format(timeFormatter) : "---");
                row.createCell(6).setCellValue(record.esRetardo() ? "Sí" : "No");
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
            
            Paragraph tituloPrincipal = new Paragraph("PODER JUDICIAL DEL ESTADO DE PUEBLA", fontTituloPrincipal);
            tituloPrincipal.setAlignment(Element.ALIGN_CENTER);

            Paragraph subtitulo = new Paragraph("SISTEMA DE ASISTENCIA", fontSubtitulo);
            subtitulo.setAlignment(Element.ALIGN_CENTER);

            Paragraph subtituloFiltrosParaPdf = new Paragraph(subtituloFiltros, fontSubtituloFiltros);
            subtituloFiltrosParaPdf.setAlignment(Element.ALIGN_CENTER);

            document.add(tituloPrincipal);
            document.add(subtitulo);
            document.add(subtituloFiltrosParaPdf);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 2f, 4f, 4f, 2f, 2f, 2f, 1.5f });

            com.lowagie.text.Font fontHeader = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD, Color.WHITE);
            String[] headers = {"Matrícula", "Nombre Completo", "Área", "Fecha", "Entrada", "Salida", "Retardo"};
            
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

            com.lowagie.text.Font fontContenido = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, Color.BLACK);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            for (AsistenciaReporteRecord record : asistencias) {
                table.addCell(new Phrase(record.usuarioMatricula(), fontContenido));
                table.addCell(new Phrase(record.usuarioNombreCompleto(), fontContenido));
                table.addCell(new Phrase(record.areaNombre(), fontContenido));
                table.addCell(new Phrase(record.fecha().toString(), fontContenido));
                table.addCell(new Phrase(record.horaEntrada() != null ? record.horaEntrada().format(timeFormatter) : "---", fontContenido));
                table.addCell(new Phrase(record.horaSalida() != null ? record.horaSalida().format(timeFormatter) : "---", fontContenido));
                table.addCell(new Phrase(record.esRetardo() ? "Sí" : "No", fontContenido));
            }

            document.add(table);
            document.close();

            return out.toByteArray();
        }
    }
}