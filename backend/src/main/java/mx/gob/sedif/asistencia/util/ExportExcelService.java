package mx.gob.sedif.asistencia.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

/**
 * Servicio genérico para generar archivos Excel (.xlsx) a partir de cualquier
 * lista de datos. Centraliza el estilo (encabezado en negrita, autoajuste de
 * columnas) para que todas las exportaciones del sistema se vean iguales.
 *
 * <p>Se usa indicando los encabezados de columna y, para cada columna, una
 * función que extrae el valor (ya formateado como texto) de cada elemento.
 */
@Service
public class ExportExcelService {

    /**
     * Genera un Excel de una sola hoja.
     *
     * @param nombreHoja  nombre de la pestaña del libro
     * @param headers     títulos de las columnas (en el orden deseado)
     * @param datos       lista de elementos a exportar
     * @param extractores una función por columna que devuelve el texto de la celda
     * @param <T>         tipo de los elementos
     * @return bytes del archivo .xlsx
     */
    public <T> byte[] generar(
            String nombreHoja,
            String[] headers,
            List<T> datos,
            List<Function<T, String>> extractores
    ) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(nombreHoja);

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
            for (T item : datos) {
                Row row = sheet.createRow(rowNum++);
                for (int col = 0; col < extractores.size(); col++) {
                    String valor = extractores.get(col).apply(item);
                    row.createCell(col).setCellValue(valor != null ? valor : "");
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
