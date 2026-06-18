package mx.gob.sedif.asistencia.core.area;

import lombok.RequiredArgsConstructor;
import mx.gob.sedif.asistencia.exception.ApiResponse;
import mx.gob.sedif.asistencia.exception.MessageConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
 * Controller REST para la gestión de áreas organizacionales.
 *
 * <p>Códigos HTTP:
 * <ul>
 *   <li>GET    → 200 OK</li>
 *   <li>POST   → 201 Created</li>
 *   <li>PUT    → 200 OK</li>
 *   <li>DELETE → 204 No Content</li>
 * </ul>
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/core/area")
@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
public class AreaResource {

    private final AreaService areaService;

    /**
     * Lista paginada de áreas filtradas por nombre.
     * HTTP 200.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AreaRecord>>> getAll(
            @RequestParam(required = false, defaultValue = "") String key,
            @PageableDefault(size = 25, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(MessageConstants.AREAS_OBTENIDAS, areaService.getAll(key, pageable)));
    }

    /**
     * Retorna un área por ID.
     * HTTP 200.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AreaRecord>> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.ok(MessageConstants.AREA_OBTENIDA, areaService.findById(id)));
    }

    /**
     * Lista completa de áreas para los select/dropdown del frontend.
     * HTTP 200.
     */
    @GetMapping("/select-list")
    public ResponseEntity<ApiResponse<List<AreaRecord>>> findAllForSelect() {
        return ResponseEntity.ok(
                ApiResponse.ok(MessageConstants.AREAS_OBTENIDAS, areaService.findAllForSelect()));
    }

    /**
     * Genera y descarga el listado de áreas activas en formato Excel (.xlsx).
     * HTTP 200 con Content-Disposition: attachment.
     */
    @GetMapping("/exportar/excel")
    public ResponseEntity<byte[]> exportarExcel() throws IOException {
        byte[] file = areaService.exportarExcel();
        String filename = "areas_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    /**
     * Crea un nuevo área organizacional.
     * HTTP 201 Created.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AreaRecord>> create(@RequestBody AreaRecord area) {
        AreaRecord creada = areaService.create(area);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageConstants.AREA_CREADA, creada));
    }

    /**
     * Actualiza los datos de un área existente.
     * HTTP 200.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AreaRecord>> save(
            @PathVariable Integer id,
            @RequestBody AreaRecord area
    ) {
        if (!id.equals(area.id())) {
            throw new IllegalArgumentException("El ID en el path no coincide con el ID en el cuerpo de la petición.");
        }
        AreaRecord actualizada = areaService.save(area);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.AREA_ACTUALIZADA, actualizada));
    }

    /**
     * Elimina un área organizacional.
     * HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteById(@PathVariable Integer id) {
        areaService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.noContent(MessageConstants.AREA_ELIMINADA));
    }
}
