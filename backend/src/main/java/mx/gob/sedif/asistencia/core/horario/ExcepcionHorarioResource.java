package mx.gob.sedif.asistencia.core.horario;

import lombok.RequiredArgsConstructor;
import mx.gob.sedif.asistencia.exception.ApiResponse;
import mx.gob.sedif.asistencia.exception.MessageConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller REST para excepciones de horario (días no laborables / días extra).
 *
 * <p>Permite a los administradores sobreescribir el horario normal de un usuario
 * para un día específico (p.ej. días festivos o guardias).
 */
@RestController
@RequestMapping("/api/core/excepciones")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
public class ExcepcionHorarioResource {

    private final ExcepcionHorarioService excepcionHorarioService;

    /**
     * Registra una excepción de horario para un usuario y fecha específicos.
     * HTTP 201 Created.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ExcepcionHorario>> crearExcepcion(
            @RequestParam Integer usuarioId,
            @RequestParam LocalDate fecha,
            @RequestParam boolean labora,
            @RequestParam(required = false) String motivo
    ) {
        ExcepcionHorario nueva = excepcionHorarioService.crearExcepcion(usuarioId, fecha, labora, motivo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageConstants.EXCEPCION_CREADA, nueva));
    }

    /**
     * Elimina una excepción de horario existente.
     * HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarExcepcion(@PathVariable Integer id) {
        excepcionHorarioService.eliminarExcepcion(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.noContent(MessageConstants.EXCEPCION_ELIMINADA));
    }
}
