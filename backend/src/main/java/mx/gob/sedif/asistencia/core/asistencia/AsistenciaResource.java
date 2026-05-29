package mx.gob.sedif.asistencia.core.asistencia;

import lombok.RequiredArgsConstructor;
import mx.gob.sedif.asistencia.core.area.AreaRepository;
import mx.gob.sedif.asistencia.core.usuario.UsuarioRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import jakarta.servlet.http.HttpServletRequest;
@RestController
@RequestMapping("/api/asistencia")
@RequiredArgsConstructor
public class AsistenciaResource {

    private final AsistenciaService asistenciaService;
    private final ReporteService reporteService;
    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;

    @PostMapping("/registrar-entrada")
    public ResponseEntity<String> registrarEntrada(@RequestBody RegistroAsistenciaRequest requestData, HttpServletRequest request) {
        String ipUsuario = request.getRemoteAddr();
        asistenciaService.registrarEntrada(requestData.fotoBase64(), ipUsuario);
        return ResponseEntity.ok("Entrada registrada con éxito");
    }

    @PostMapping("/registrar-salida")
    public ResponseEntity<String> registrarSalida(@RequestBody RegistroAsistenciaRequest requestData, HttpServletRequest request) {
        String ipUsuario = request.getRemoteAddr();
        asistenciaService.registrarSalida(requestData.fotoBase64(), ipUsuario);
        return ResponseEntity.ok("Salida registrada con éxito");
    }

    @GetMapping("/estado-diario")
    public ResponseEntity<Map<String, Boolean>> getEstadoDiario(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(asistenciaService.getEstadoAsistenciaDiario(userDetails.getUsername()));
    }

    @GetMapping("/reporte")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public Page<AsistenciaReporteRecord> getReporteAsistencias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false, defaultValue = "") String key,
            @PageableDefault(size = 25, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {

        return asistenciaService.getReporteAsistencias(
                Optional.ofNullable(fechaInicio), Optional.ofNullable(fechaFin),
                Optional.ofNullable(usuarioId), Optional.ofNullable(areaId),
                Optional.of(key), pageable);
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public AsistenciaReporteRecord createManual(@RequestBody AsistenciaManualRecord record) {
        return asistenciaService.createManual(record);
    }

    @PutMapping("/manual/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public AsistenciaReporteRecord updateManual(@PathVariable Long id, @RequestBody AsistenciaManualRecord record) {
        return asistenciaService.updateManual(id, record);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public void deleteById(@PathVariable Long id) {
        asistenciaService.deleteById(id);
    }

    @GetMapping("/exportar/excel")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false, defaultValue = "") String key,
            @RequestParam(required = false, defaultValue = "false") Boolean soloRetardos) throws IOException {

        List<AsistenciaReporteRecord> asistencias = asistenciaService.getReporteData(
                Optional.ofNullable(fechaInicio), Optional.ofNullable(fechaFin),
                Optional.ofNullable(usuarioId), Optional.ofNullable(areaId),
                Optional.of(key), Optional.ofNullable(soloRetardos));

        byte[] excelFile = reporteService.generarReporteExcel(asistencias);
        String filename = "reporte_asistencias_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelFile);
    }

    @GetMapping("/exportar/pdf")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false, defaultValue = "") String key,
            @RequestParam(required = false, defaultValue = "false") Boolean soloRetardos) throws IOException {

        List<AsistenciaReporteRecord> asistencias = asistenciaService.getReporteData(
                Optional.ofNullable(fechaInicio), Optional.ofNullable(fechaFin),
                Optional.ofNullable(usuarioId), Optional.ofNullable(areaId),
                Optional.of(key), Optional.ofNullable(soloRetardos));

        String subtitulo = generarSubtituloDinamico(
                Optional.ofNullable(fechaInicio), Optional.ofNullable(fechaFin),
                Optional.ofNullable(usuarioId), Optional.ofNullable(areaId),
                Optional.ofNullable(soloRetardos));

        byte[] pdfFile = reporteService.generarReportePdf(asistencias, subtitulo);
        String filename = "reporte_asistencias_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
    }

    private String generarSubtituloDinamico(Optional<LocalDate> fechaInicio, Optional<LocalDate> fechaFin,
                                            Optional<Integer> usuarioId, Optional<Integer> areaId,
                                            Optional<Boolean> soloRetardos) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        Stream<String> filtros = Stream.of(
            fechaInicio.map(f -> "Desde: " + f.format(formatter)),
            fechaFin.map(f -> "Hasta: " + f.format(formatter)),
            
            usuarioId.flatMap(usuarioRepository::findById)
                    .map(usuario -> "Usuario: " + usuario.getNombre() + " " + usuario.getApellidoPaterno()),
            
            areaId.flatMap(areaRepository::findById)
                .map(area -> "Área: " + area.getNombre()),
            
            soloRetardos.filter(Boolean::booleanValue).map(b -> "Solo Retardos")
        ).flatMap(Optional::stream);

        String descripcionFiltros = filtros.collect(Collectors.joining(" - "));
        return descripcionFiltros.isEmpty() ? "Reporte General" : "Filtros Aplicados: " + descripcionFiltros;
    }


    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcelMasivo(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> resultado = asistenciaService.procesarCargaMasivaExcel(file);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error al procesar el archivo: " + e.getMessage()));
        }
    }
}