package mx.gob.pjpuebla.asistencia.core.horario;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/horario")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')") // Solo el Superadmin puede gestionar horarios
public class HorarioResource {

    private final HorarioService horarioService;

    @GetMapping
    public Page<HorarioRecord> getAll(Pageable pageable) {
        return horarioService.getAll(pageable);
    }

    @PostMapping
    public HorarioRecord create(@RequestBody HorarioRecord record) {
        return horarioService.create(record);
    }

    @PutMapping("/{id}")
    public HorarioRecord save(@PathVariable Integer id, @RequestBody HorarioRecord record) {
        return horarioService.save(record);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Integer id) {
        horarioService.deleteById(id);
    }
}