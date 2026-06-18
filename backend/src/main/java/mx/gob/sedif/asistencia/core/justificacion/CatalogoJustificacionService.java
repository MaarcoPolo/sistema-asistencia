package mx.gob.sedif.asistencia.core.justificacion;

import lombok.RequiredArgsConstructor;
import mx.gob.sedif.asistencia.util.ExportExcelService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogoJustificacionService {

    private final CatalogoJustificacionRepository repository;
    private final ExportExcelService exportExcelService;

    @Transactional(readOnly = true)
    public Page<CatalogoJustificacionRecord> getAll(String key, Pageable pageable) {
        return repository.findAllWithSearch(key, pageable).map(this::toRecord);
    }

    /**
     * Genera el Excel del catálogo de justificaciones (campos clave y nombre).
     */
    @Transactional(readOnly = true)
    public byte[] exportarExcel() throws IOException {
        List<CatalogoJustificacion> justificaciones = repository.findAll().stream()
                .sorted(Comparator.comparing(CatalogoJustificacion::getNombre, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        String[] headers = { "Clave", "Nombre" };
        List<Function<CatalogoJustificacion, String>> extractores = List.of(
                CatalogoJustificacion::getClave,
                CatalogoJustificacion::getNombre
        );

        return exportExcelService.generar("Justificaciones", headers, justificaciones, extractores);
    }

    @Transactional(readOnly = true)
    public List<CatalogoJustificacionRecord> findAllForSelect() {
        return repository.findAll().stream().map(this::toRecord).collect(Collectors.toList());
    }

    @Transactional
    public CatalogoJustificacionRecord create(CatalogoJustificacionRecord record) {
        CatalogoJustificacion entity = new CatalogoJustificacion();
        mapToEntity(record, entity);
        return toRecord(repository.save(entity));
    }

    @Transactional
    public CatalogoJustificacionRecord save(CatalogoJustificacionRecord record) {
        CatalogoJustificacion entity = repository.findById(record.id())
                .orElseThrow(() -> new RuntimeException("Justificación no encontrada"));
        mapToEntity(record, entity);
        return toRecord(repository.save(entity));
    }

    @Transactional
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    private CatalogoJustificacionRecord toRecord(CatalogoJustificacion entity) {
        return new CatalogoJustificacionRecord(
                entity.getId(), entity.getClave(), entity.getNombre(), entity.getRequiereObservacion()
        );
    }

    private void mapToEntity(CatalogoJustificacionRecord record, CatalogoJustificacion entity) {
        entity.setClave(record.clave());
        entity.setNombre(record.nombre());
        entity.setRequiereObservacion(record.requiereObservacion() != null ? record.requiereObservacion() : false);
    }
}