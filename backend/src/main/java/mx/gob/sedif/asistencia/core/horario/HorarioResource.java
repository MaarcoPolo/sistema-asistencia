package mx.gob.sedif.asistencia.core.horario;

import lombok.RequiredArgsConstructor;
import mx.gob.sedif.asistencia.exception.ApiResponse;
import mx.gob.sedif.asistencia.exception.MessageConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para la gestión de horarios de trabajo.
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
@RequestMapping("/api/core/horario")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
public class HorarioResource {

    private final HorarioService horarioService;

    /**
     * Lista paginada de horarios filtrados por nombre.
     * HTTP 200.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<HorarioRecord>>> getAll(
            @RequestParam(required = false, defaultValue = "") String key,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(MessageConstants.HORARIOS_OBTENIDOS, horarioService.getAll(key, pageable)));
    }

    /**
     * Crea un nuevo horario de trabajo.
     * HTTP 201 Created.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<HorarioRecord>> create(@RequestBody HorarioRecord record) {
        HorarioRecord creado = horarioService.create(record);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageConstants.HORARIO_CREADO, creado));
    }

    /**
     * Actualiza un horario de trabajo existente.
     * HTTP 200.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HorarioRecord>> save(
            @PathVariable Integer id,
            @RequestBody HorarioRecord record
    ) {
        if (!id.equals(record.id())) {
            throw new IllegalArgumentException("El ID en el path no coincide con el ID en el cuerpo de la petición.");
        }
        HorarioRecord actualizado = horarioService.save(record);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.HORARIO_ACTUALIZADO, actualizado));
    }

    /**
     * Elimina un horario de trabajo.
     * HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteById(@PathVariable Integer id) {
        horarioService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.noContent(MessageConstants.HORARIO_ELIMINADO));
    }
}
