package mx.gob.pjpuebla.asistencia.core.horario;

import lombok.RequiredArgsConstructor;
import mx.gob.pjpuebla.asistencia.core.area.AreaRepository;
import mx.gob.pjpuebla.asistencia.core.usuario.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HorarioService {

    private final HorarioRepository horarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;

    @Transactional(readOnly = true)
    public Page<HorarioRecord> getAll(Pageable pageable) {
        return horarioRepository.findAll(pageable).map(this::toRecord);
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
        return new HorarioRecord(
            entity.getId(),
            entity.getNombre(),
            entity.getHoraEntrada(),
            entity.getHoraSalida(),
            entity.getToleranciaMinutos(),
            entity.getUsuario() != null ? entity.getUsuario().getId() : null,
            entity.getArea() != null ? entity.getArea().getId() : null,
            entity.getUsuario() != null ? (entity.getUsuario().getNombre() + " " + entity.getUsuario().getApellidoPaterno()) : null,
            entity.getArea() != null ? entity.getArea().getNombre() : null
        );
    }

    private void mapToEntity(HorarioRecord record, Horario entity) {
        entity.setNombre(record.nombre());
        entity.setHoraEntrada(record.horaEntrada());
        entity.setHoraSalida(record.horaSalida());
        entity.setToleranciaMinutos(record.toleranciaMinutos());

        entity.setUsuario(null);
        if (record.idUsuario() != null) {
            entity.setUsuario(usuarioRepository.findById(record.idUsuario()).orElse(null));
        }

        entity.setArea(null);
        if (record.idArea() != null) {
            entity.setArea(areaRepository.findById(record.idArea()).orElse(null));
        }
    }
}