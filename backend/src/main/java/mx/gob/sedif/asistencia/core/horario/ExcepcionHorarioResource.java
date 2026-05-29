package mx.gob.sedif.asistencia.core.horario;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/core/excepciones")
@RequiredArgsConstructor
public class ExcepcionHorarioResource {

    private final ExcepcionHorarioService excepcionHorarioService;

    // Solo los administradores pueden registrar excepciones
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PostMapping
    public ResponseEntity<?> crearExcepcion(
            @RequestParam Integer usuarioId,
            @RequestParam LocalDate fecha,
            @RequestParam boolean labora,
            @RequestParam(required = false) String motivo) {
        
        try {
            ExcepcionHorario nuevaExcepcion = excepcionHorarioService.crearExcepcion(usuarioId, fecha, labora, motivo);
            return ResponseEntity.ok(nuevaExcepcion);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarExcepcion(@PathVariable Integer id) {
        try {
            excepcionHorarioService.eliminarExcepcion(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}