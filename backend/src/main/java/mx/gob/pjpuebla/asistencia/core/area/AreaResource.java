package mx.gob.pjpuebla.asistencia.core.area;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/core/area")
public class AreaResource {
    private final AreaService areaService;

    @GetMapping
    public Page<AreaRecord> getAll(
            @RequestParam(required = false, defaultValue = "") String key,
            @PageableDefault(size = 25, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return areaService.getAll(key, pageable);
    }

    @GetMapping("/{id}")
    public AreaRecord findById(@PathVariable Integer id) {
        return areaService.findById(id);
    }

    @PostMapping
    public AreaRecord create(@RequestBody AreaRecord area) {
        return areaService.create(area);
    }

    @PutMapping("/{id}")
    public AreaRecord save(@PathVariable Integer id, @RequestBody AreaRecord area) {
        if (!id.equals(area.id())) {
            throw new IllegalArgumentException("El ID en el path no coincide con el ID en el cuerpo de la petición.");
        }
        return areaService.save(area);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Integer id) {
        areaService.deleteById(id);
    }

    @GetMapping("/select-list")
    public List<AreaRecord> findAllForSelect() {
        return areaService.findAllForSelect();
    }
}