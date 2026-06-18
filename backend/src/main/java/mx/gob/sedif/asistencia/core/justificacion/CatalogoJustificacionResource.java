package mx.gob.sedif.asistencia.core.justificacion;

import lombok.RequiredArgsConstructor;
import mx.gob.sedif.asistencia.exception.ApiResponse;
import mx.gob.sedif.asistencia.exception.MessageConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller REST para el catálogo de motivos de justificación de asistencia.
 *
 * <p>Códigos HTTP:
 * <ul>
 *   <li>GET    → 200 OK</li>
 *   <li>POST   → 201 Created</li>
 *   <li>PUT    → 200 OK</li>
 *   <li>DELETE → 204 No Content</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/core/justificacion")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
public class CatalogoJustificacionResource {

    private final CatalogoJustificacionService service;

    /**
     * Lista paginada de motivos de justificación filtrados por nombre.
     * HTTP 200.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CatalogoJustificacionRecord>>> getAll(
            @RequestParam(required = false, defaultValue = "") String key,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(MessageConstants.JUSTIFICACIONES_OBTENIDAS, service.getAll(key, pageable)));
    }

    /**
     * Lista completa de motivos activos para los select/dropdown del frontend.
     * Accesible por todos los roles autenticados.
     * HTTP 200.
     */
    @GetMapping("/select-list")
    @PreAuthorize("hasAnyRole('USER', 'SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CatalogoJustificacionRecord>>> findAllForSelect() {
        return ResponseEntity.ok(
                ApiResponse.ok(MessageConstants.JUSTIFICACIONES_OBTENIDAS, service.findAllForSelect()));
    }

    /**
     * Genera y descarga el catálogo de justificaciones en formato Excel (.xlsx).
     * HTTP 200 con Content-Disposition: attachment.
     */
    @GetMapping("/exportar/excel")
    public ResponseEntity<byte[]> exportarExcel() throws IOException {
        byte[] file = service.exportarExcel();
        String filename = "justificaciones_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    /**
     * Crea un nuevo motivo de justificación.
     * HTTP 201 Created.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CatalogoJustificacionRecord>> create(
            @RequestBody CatalogoJustificacionRecord record
    ) {
        CatalogoJustificacionRecord creado = service.create(record);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageConstants.JUSTIFICACION_CREADA, creado));
    }

    /**
     * Actualiza un motivo de justificación existente.
     * HTTP 200.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CatalogoJustificacionRecord>> save(
            @PathVariable Integer id,
            @RequestBody CatalogoJustificacionRecord record
    ) {
        CatalogoJustificacionRecord actualizado = service.save(record);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.JUSTIFICACION_ACTUALIZADA, actualizado));
    }

    /**
     * Elimina un motivo de justificación.
     * HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteById(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.noContent(MessageConstants.JUSTIFICACION_ELIMINADA));
    }
}
