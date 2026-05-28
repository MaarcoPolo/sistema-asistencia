package mx.gob.sedif.asistencia.core.justificacion;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/core/justificacion")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
public class CatalogoJustificacionResource {

    private final CatalogoJustificacionService service;

    @GetMapping
    public Page<CatalogoJustificacionRecord> getAll(@RequestParam(required = false, defaultValue = "") String key, Pageable pageable) {
        return service.getAll(key, pageable);
    }

    @GetMapping("/select-list")
    public List<CatalogoJustificacionRecord> findAllForSelect() {
        return service.findAllForSelect();
    }

    @PostMapping
    public CatalogoJustificacionRecord create(@RequestBody CatalogoJustificacionRecord record) {
        return service.create(record);
    }

    @PutMapping("/{id}")
    public CatalogoJustificacionRecord save(@PathVariable Integer id, @RequestBody CatalogoJustificacionRecord record) {
        return service.save(record);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Integer id) {
        service.deleteById(id);
    }
}