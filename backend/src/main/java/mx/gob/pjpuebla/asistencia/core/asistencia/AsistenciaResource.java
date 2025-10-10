package mx.gob.pjpuebla.asistencia.core.asistencia;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/api/asistencia")
@RequiredArgsConstructor
public class AsistenciaResource {

    private final AsistenciaService asistenciaService;

    @PostMapping("/registrar-entrada")
    public ResponseEntity<Void> registrarEntrada(@RequestBody RegistroAsistenciaRequest request) {
        asistenciaService.registrarEntrada(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/registrar-salida")
    public ResponseEntity<Void> registrarSalida(@RequestBody RegistroAsistenciaRequest request) {
        asistenciaService.registrarSalida(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/reporte")
    public Page<AsistenciaReporteRecord> getReporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> fechaFin,
            @RequestParam Optional<Integer> usuarioId,
            @RequestParam Optional<Integer> areaId,
            @PageableDefault(size = 25, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return asistenciaService.getReporteAsistencias(fechaInicio, fechaFin, usuarioId, areaId, pageable);
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public AsistenciaReporteRecord createManual(@RequestBody AsistenciaManualRecord record) {
        return asistenciaService.createManual(record);
    }

    @PutMapping("/manual/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public AsistenciaReporteRecord updateManual(@PathVariable Long id, @RequestBody AsistenciaManualRecord record) {
        return asistenciaService.updateManual(id, record);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public void deleteById(@PathVariable Long id) {
        asistenciaService.deleteById(id);
    }

    @GetMapping("/estado-diario")
    @PreAuthorize("hasRole('USER')") // Aseguramos que solo los usuarios normales puedan acceder
    public ResponseEntity<Map<String, Boolean>> getEstadoAsistenciaDiario(
            @AuthenticationPrincipal UserDetails userDetails) {
        String matricula = userDetails.getUsername();
        Map<String, Boolean> estado = asistenciaService.getEstadoAsistenciaDiario(matricula);
        return ResponseEntity.ok(estado);
    }
}