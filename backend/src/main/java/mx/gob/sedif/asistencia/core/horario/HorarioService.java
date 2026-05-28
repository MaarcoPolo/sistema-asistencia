package mx.gob.sedif.asistencia.core.horario;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HorarioService {

    private final HorarioRepository horarioRepository;

    @Transactional(readOnly = true)
    public Page<HorarioRecord> getAll(String key, Pageable pageable) {
        return horarioRepository.findAllWithSearch(key, pageable).map(this::toRecord);
    }

    @Transactional
    public HorarioRecord create(HorarioRecord record) {
        Horario entity = new Horario();
        mapToEntity(record, entity);
        entity = horarioRepository.save(entity);
        return toRecord(entity);
    }

    @Transactional
    public HorarioRecord save(HorarioRecord record) {
        Horario entity = horarioRepository.findById(record.id())
            .orElseThrow(() -> new RuntimeException("Horario no encontrado"));
        
        mapToEntity(record, entity);
        entity = horarioRepository.save(entity);
        return toRecord(entity);
    }

    @Transactional
    public void deleteById(Integer id) {
        if (!horarioRepository.existsById(id)) {
            throw new RuntimeException("Horario no encontrado para eliminar");
        }
        horarioRepository.deleteById(id);
    }

    private HorarioRecord toRecord(Horario entity) {
        List<HorarioDetalleRecord> detallesRecord = entity.getDetalles().stream()
            .map(d -> new HorarioDetalleRecord(
                d.getId(), 
                d.getDia(), 
                d.getHoraEntrada(), 
                d.getHoraSalida(), 
                d.getToleranciaMinutos()
            ))
            .collect(Collectors.toList());

        return new HorarioRecord(
            entity.getId(),
            entity.getNombre(),
            entity.getCruceMedianoche(),
            entity.getTipoCiclo(),
            detallesRecord
        );
    }

    private void mapToEntity(HorarioRecord record, Horario entity) {
        entity.setNombre(record.nombre());
        entity.setCruceMedianoche(record.cruceMedianoche() != null ? record.cruceMedianoche() : false);
        entity.setTipoCiclo(record.tipoCiclo() != null ? record.tipoCiclo() : 1);

        // Limpiamos los detalles actuales y asignamos los nuevos
        // Gracias a orphanRemoval = true, JPA borrará de la BD los días que se quiten
        entity.getDetalles().clear();
        
        if (record.detalles() != null) {
            for (HorarioDetalleRecord detRecord : record.detalles()) {
                HorarioDetalle detalle = new HorarioDetalle();
                detalle.setHorario(entity); // Asociamos el detalle a su maestro
                detalle.setDia(detRecord.dia());
                detalle.setHoraEntrada(detRecord.horaEntrada());
                detalle.setHoraSalida(detRecord.horaSalida());
                detalle.setToleranciaMinutos(detRecord.toleranciaMinutos() != null ? detRecord.toleranciaMinutos() : 15);
                
                entity.getDetalles().add(detalle);
            }
        }
    }
}