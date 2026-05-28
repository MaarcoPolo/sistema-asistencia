package mx.gob.sedif.asistencia.core.justificacion;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogoJustificacionService {

    private final CatalogoJustificacionRepository repository;

    @Transactional(readOnly = true)
    public Page<CatalogoJustificacionRecord> getAll(String key, Pageable pageable) {
        return repository.findAllWithSearch(key, pageable).map(this::toRecord);
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