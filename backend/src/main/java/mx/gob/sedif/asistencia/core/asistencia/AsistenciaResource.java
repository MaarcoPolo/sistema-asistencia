package mx.gob.sedif.asistencia.core.asistencia;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mx.gob.sedif.asistencia.core.area.AreaRepository;
import mx.gob.sedif.asistencia.core.justificacion.JustificarAsistenciaRequest;
import mx.gob.sedif.asistencia.core.usuario.UsuarioRepository;
import mx.gob.sedif.asistencia.exception.ApiResponse;
import mx.gob.sedif.asistencia.exception.MessageConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Controller REST para todos los endpoints del módulo de asistencia.
 *
 * <p>Códigos HTTP:
 * <ul>
 *   <li>GET    → 200 OK</li>
 *   <li>POST registro/manual → 201 Created</li>
 *   <li>PUT    → 200 OK</li>
 *   <li>DELETE → 204 No Content</li>
 *   <li>Exportaciones → 200 con body binario</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/asistencia")
@RequiredArgsConstructor
public class AsistenciaResource {

    private final AsistenciaService asistenciaService;
    private final ReporteService reporteService;
    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;

    /**
     * Registra la entrada del usuario autenticado para el día actual.
     * HTTP 201 Created.
     */
    @PostMapping("/registrar-entrada")
    public ResponseEntity<ApiResponse<Void>> registrarEntrada(
            @RequestBody RegistroAsistenciaRequest requestData,
            HttpServletRequest request
    ) {
        asistenciaService.registrarEntrada(requestData.fotoBase64(), request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageConstants.ENTRADA_REGISTRADA, null));
    }

    /**
     * Registra la salida del usuario autenticado (soporta turno nocturno con cruce de medianoche).
     * HTTP 201 Created.
     */
    @PostMapping("/registrar-salida")
    public ResponseEntity<ApiResponse<Void>> registrarSalida(
            @RequestBody RegistroAsistenciaRequest requestData,
            HttpServletRequest request
    ) {
        asistenciaService.registrarSalida(requestData.fotoBase64(), request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageConstants.SALIDA_REGISTRADA, null));
    }

    /**
     * Retorna si el usuario autenticado ya registró entrada y/o salida hoy.
     * HTTP 200.
     */
    @GetMapping("/estado-diario")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getEstadoDiario(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Map<String, Boolean> estado = asistenciaService.getEstadoAsistenciaDiario(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.ESTADO_DIARIO_OBTENIDO, estado));
    }

    /**
     * Retorna página de asistencias con filtros opcionales para administradores.
     * HTTP 200.
     */
    @GetMapping("/reporte")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<AsistenciaReporteRecord>>> getReporteAsistencias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false, defaultValue = "") String key,
            @PageableDefault(size = 25, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AsistenciaReporteRecord> resultado = asistenciaService.getReporteAsistencias(
                Optional.ofNullable(fechaInicio), Optional.ofNullable(fechaFin),
                Optional.ofNullable(usuarioId), Optional.ofNullable(areaId),
                Optional.of(key), pageable);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.REPORTE_OBTENIDO, resultado));
    }

    /**
     * Crea un registro de asistencia manual para Recursos Humanos.
     * HTTP 201 Created.
     */
    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<AsistenciaReporteRecord>> createManual(
            @RequestBody AsistenciaManualRecord record
    ) {
        AsistenciaReporteRecord creado = asistenciaService.createManual(record);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageConstants.ASISTENCIA_CREADA, creado));
    }

    /**
     * Actualiza un registro de asistencia existente (manual).
     * HTTP 200.
     */
    @PutMapping("/manual/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<AsistenciaReporteRecord>> updateManual(
            @PathVariable Long id,
            @RequestBody AsistenciaManualRecord record
    ) {
        AsistenciaReporteRecord actualizado = asistenciaService.updateManual(id, record);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.ASISTENCIA_ACTUALIZADA, actualizado));
    }

    /**
     * Elimina físicamente un registro de asistencia.
     * HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteById(@PathVariable Long id) {
        asistenciaService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.noContent(MessageConstants.ASISTENCIA_ELIMINADA));
    }

    /**
     * Genera y descarga el reporte de asistencias en formato Excel (.xlsx).
     * HTTP 200 con Content-Disposition: attachment.
     */
    @GetMapping("/exportar/excel")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false, defaultValue = "") String key,
            @RequestParam(required = false, defaultValue = "false") Boolean soloRetardos
    ) throws IOException {
        List<AsistenciaReporteRecord> asistencias = asistenciaService.getReporteData(
                Optional.ofNullable(fechaInicio), Optional.ofNullable(fechaFin),
                Optional.ofNullable(usuarioId), Optional.ofNullable(areaId),
                Optional.of(key), Optional.ofNullable(soloRetardos));

        byte[] file = reporteService.generarReporteExcel(asistencias);
        String filename = "reporte_asistencias_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    /**
     * Genera y descarga el reporte de asistencias en formato PDF.
     * HTTP 200 con Content-Disposition: attachment.
     */
    @GetMapping("/exportar/pdf")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false, defaultValue = "") String key,
            @RequestParam(required = false, defaultValue = "false") Boolean soloRetardos
    ) throws IOException {
        List<AsistenciaReporteRecord> asistencias = asistenciaService.getReporteData(
                Optional.ofNullable(fechaInicio), Optional.ofNullable(fechaFin),
                Optional.ofNullable(usuarioId), Optional.ofNullable(areaId),
                Optional.of(key), Optional.ofNullable(soloRetardos));

        String subtitulo = generarSubtituloDinamico(
                Optional.ofNullable(fechaInicio), Optional.ofNullable(fechaFin),
                Optional.ofNullable(usuarioId), Optional.ofNullable(areaId),
                Optional.ofNullable(soloRetardos));

        byte[] file = reporteService.generarReportePdf(asistencias, subtitulo);
        String filename = "reporte_asistencias_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }

    /**
     * Procesa un archivo Excel con registros masivos de asistencia desde biométricos.
     * HTTP 200 con el resumen de procesados y errores.
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadExcelMasivo(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        Map<String, Object> resultado = asistenciaService.procesarCargaMasivaExcel(file);
        int procesados = (int) resultado.get("procesados");
        int errores = (int) resultado.get("errores");
        String mensaje = String.format(MessageConstants.EXCEL_PROCESADO, procesados, errores);
        return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
    }

    /**
     * Calcula el resumen de sanciones (descuentos de días) para el período indicado.
     * HTTP 200.
     */
    @GetMapping("/resumen-sanciones")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ResumenSancionesRecord>>> getResumenSanciones(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) Integer areaId
    ) {
        List<ResumenSancionesRecord> sanciones = asistenciaService.calcularSanciones(
                fechaInicio, fechaFin, Optional.ofNullable(usuarioId), Optional.ofNullable(areaId));
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.SANCIONES_CALCULADAS, sanciones));
    }

    /**
     * Descarga el reporte de sanciones en formato PDF.
     * HTTP 200 con Content-Disposition: attachment.
     */
    @GetMapping("/exportar/sanciones/pdf")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<byte[]> exportarSancionesPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) Integer areaId
    ) throws IOException {
        List<ResumenSancionesRecord> sanciones = asistenciaService.calcularSanciones(
                fechaInicio, fechaFin, Optional.ofNullable(usuarioId), Optional.ofNullable(areaId));

        byte[] file = reporteService.generarReporteSancionesPdf(sanciones,
                fechaInicio + " al " + fechaFin);
        String filename = "Sanciones_Asistencia_" + fechaInicio + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }

    /**
     * Descarga el reporte de sanciones en formato Excel.
     * HTTP 200 con Content-Disposition: attachment.
     */
    @GetMapping("/exportar/sanciones/excel")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<byte[]> exportarSancionesExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) Integer areaId
    ) throws IOException {
        List<ResumenSancionesRecord> sanciones = asistenciaService.calcularSanciones(
                fechaInicio, fechaFin, Optional.ofNullable(usuarioId), Optional.ofNullable(areaId));

        byte[] file = reporteService.generarReporteSancionesExcel(sanciones,
                fechaInicio + " al " + fechaFin);
        String filename = "Sanciones_Asistencia_" + fechaInicio + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    /**
     * Aplica una justificación a una incidencia de asistencia (uso de administrador).
     * HTTP 200.
     */
    @PostMapping("/{id}/justificar")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> justificarAsistencia(
            @PathVariable Long id,
            @RequestBody JustificarAsistenciaRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        asistenciaService.justificarAsistencia(id, request.justificacionId(),
                request.observacion(), userDetails.getUsername(),mx.gob.sedif.asistencia.util.enums.EstatusJustificacion.APROBADA);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.JUSTIFICACION_APLICADA));
    }

    /**
     * Retorna el historial paginado de asistencias del usuario autenticado.
     * HTTP 200.
     */
    @GetMapping("/mis-asistencias")
    @PreAuthorize("hasAnyRole('USER', 'SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<AsistenciaReporteRecord>>> getMisAsistencias(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AsistenciaReporteRecord> mis = asistenciaService.getMisAsistencias(
                userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.REPORTE_OBTENIDO, mis));
    }

    /**
     * Permite al usuario básico justificar una de sus propias incidencias.
     * HTTP 200.
     */
    @PostMapping("/{id}/mi-justificacion")
    @PreAuthorize("hasAnyRole('USER', 'SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> justificarMiAsistencia(
            @PathVariable Long id,
            @RequestBody JustificarAsistenciaRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        asistenciaService.justificarMiAsistencia(id, request.justificacionId(),
                request.observacion(), userDetails.getUsername());
        // El empleado deja la justificación PENDIENTE; el mensaje refleja que espera aprobación.
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.JUSTIFICACION_ENVIADA));
    }

    // ── Métodos privados auxiliares ───────────────────────────────────────

    /**
     * Genera el subtítulo descriptivo del reporte PDF combinando los filtros activos.
     */
    private String generarSubtituloDinamico(
            Optional<LocalDate> fechaInicio, Optional<LocalDate> fechaFin,
            Optional<Integer> usuarioId, Optional<Integer> areaId,
            Optional<Boolean> soloRetardos
    ) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Stream<String> filtros = Stream.of(
                fechaInicio.map(f -> "Desde: " + f.format(formatter)),
                fechaFin.map(f -> "Hasta: " + f.format(formatter)),
                usuarioId.flatMap(usuarioRepository::findById)
                        .map(u -> "Usuario: " + u.getNombre() + " " + u.getApellidoPaterno()),
                areaId.flatMap(areaRepository::findById)
                        .map(a -> "Área: " + a.getNombre()),
                soloRetardos.filter(Boolean::booleanValue).map(b -> "Solo Retardos")
        ).flatMap(Optional::stream);

        String descripcion = filtros.collect(Collectors.joining(" - "));
        return descripcion.isEmpty() ? "Reporte General" : "Filtros Aplicados: " + descripcion;
    }

    /**
     * Aprueba una justificación pendiente (solo administradores).
     * HTTP 200.
     */
    @PostMapping("/{id}/justificacion/aprobar")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> aprobarJustificacion(@PathVariable Long id) {
        asistenciaService.aprobarJustificacion(id);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.JUSTIFICACION_APROBADA));
    }

    /**
     * Rechaza una justificación pendiente (solo administradores).
     * Tras el rechazo el empleado podrá volver a justificar la incidencia.
     * HTTP 200.
     */
    @PostMapping("/{id}/justificacion/rechazar")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rechazarJustificacion(@PathVariable Long id) {
        asistenciaService.rechazarJustificacion(id);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.JUSTIFICACION_RECHAZADA));
    }
}
