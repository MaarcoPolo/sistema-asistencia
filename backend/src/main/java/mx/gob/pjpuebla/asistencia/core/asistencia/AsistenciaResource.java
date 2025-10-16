package mx.gob.pjpuebla.asistencia.core.asistencia;

import lombok.RequiredArgsConstructor;
import mx.gob.pjpuebla.asistencia.core.area.AreaRepository;
import mx.gob.pjpuebla.asistencia.core.usuario.UsuarioRepository;
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
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> registrarEntrada(@RequestParam("file") MultipartFile foto, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        asistenciaService.registrarEntrada(foto,ipAddress);
        return ResponseEntity.ok(Map.of("message", "Entrada registrada con éxito."));
    }

    @PostMapping("/registrar-salida")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> registrarSalida(@RequestParam("file") MultipartFile foto, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        asistenciaService.registrarSalida(foto, ipAddress);
        return ResponseEntity.ok(Map.of("message", "Salida registrada con éxito."));
    }

    @GetMapping("/reporte")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public Page<AsistenciaReporteRecord> getReporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> fechaFin,
            @RequestParam Optional<Integer> usuarioId,
            @RequestParam Optional<Integer> areaId,
            @RequestParam Optional<String> key,
            @PageableDefault(size = 25, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return asistenciaService.getReporteAsistencias(fechaInicio, fechaFin, usuarioId, areaId, key, pageable);
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

    @GetMapping("/estado-diario")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Boolean>> getEstadoAsistenciaDiario(
            @AuthenticationPrincipal UserDetails userDetails) {
        String matricula = userDetails.getUsername();
        Map<String, Boolean> estado = asistenciaService.getEstadoAsistenciaDiario(matricula);
        return ResponseEntity.ok(estado);
    }

    @GetMapping("/exportar/excel")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<byte[]> exportarAExcel(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> fechaInicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> fechaFin,
        @RequestParam Optional<Integer> usuarioId,
        @RequestParam Optional<Integer> areaId,
        @RequestParam Optional<String> key,
        @RequestParam Optional<Boolean> soloRetardos
    ) throws IOException {

        List<AsistenciaReporteRecord> data = asistenciaService.getReporteData(fechaInicio, fechaFin, usuarioId, areaId, key, soloRetardos);
        byte[] excelFile = reporteService.generarReporteExcel(data);

        String filename = "Reporte_Asistencias_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelFile);
    }

    @GetMapping("/exportar/pdf")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<byte[]> exportarAPdf(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> fechaInicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> fechaFin,
        @RequestParam Optional<Integer> usuarioId,
        @RequestParam Optional<Integer> areaId,
        @RequestParam Optional<String> key,
        @RequestParam Optional<Boolean> soloRetardos
    ) throws IOException {

        List<AsistenciaReporteRecord> data = asistenciaService.getReporteData(fechaInicio, fechaFin, usuarioId, areaId, key, soloRetardos);
        
        String subtitulo = generarSubtituloDinamico(fechaInicio, fechaFin, usuarioId, areaId, soloRetardos);

        byte[] pdfFile = reporteService.generarReportePdf(data, subtitulo);

        String filename = "Reporte_Asistencias_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

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
            
            // Si hay un usuarioId, lo buscamos y obtenemos su nombre completo.
            usuarioId.flatMap(usuarioRepository::findById)
                    .map(usuario -> "Usuario: " + usuario.getNombre() + " " + usuario.getApellidoPaterno()),
            
            // Si hay un areaId, la buscamos y obtenemos su nombre.
            areaId.flatMap(areaRepository::findById)
                .map(area -> "Área: " + area.getNombre()),
            
            soloRetardos.filter(Boolean::booleanValue).map(b -> "Solo Retardos")
        ).flatMap(Optional::stream);

        String descripcionFiltros = filtros.collect(Collectors.joining(" - "));
        return descripcionFiltros.isEmpty() ? "Reporte General" : "Filtros Aplicados: " + descripcionFiltros;
    }
}