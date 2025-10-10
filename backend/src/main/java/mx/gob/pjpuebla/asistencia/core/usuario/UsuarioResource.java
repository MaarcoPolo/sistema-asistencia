package mx.gob.pjpuebla.asistencia.core.usuario;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/usuario")
@RequiredArgsConstructor
public class UsuarioResource {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public Page<UsuarioRecord> getAll(
            @RequestParam(required = false, defaultValue = "") String key,
            @PageableDefault(size = 25, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return usuarioService.getAll(key, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public UsuarioRecord findById(@PathVariable Integer id) {
        return usuarioService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public UsuarioRecord create(@RequestBody UsuarioRecord usuarioRecord) {
        return usuarioService.create(usuarioRecord);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public UsuarioRecord save(@PathVariable Integer id, @RequestBody UsuarioRecord usuarioRecord) {
        if (!id.equals(usuarioRecord.id())) {
            throw new IllegalArgumentException("El ID del path y del body no coinciden.");
        }
        return usuarioService.save(usuarioRecord);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public void deleteById(@PathVariable Integer id) {
        usuarioService.deleteById(id);
    }
}