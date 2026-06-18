package mx.gob.sedif.asistencia.core.horario;

import lombok.RequiredArgsConstructor;
import mx.gob.sedif.asistencia.util.ExportExcelService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HorarioService {

    private final HorarioRepository horarioRepository;
    private final ExportExcelService exportExcelService;

    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** Nombre del día a partir del número (1=Lunes ... 7=Domingo). */
    private static String nombreDia(Integer dia) {
        if (dia == null) return "";
        return switch (dia) {
            case 1 -> "Lunes";
            case 2 -> "Martes";
            case 3 -> "Miércoles";
            case 4 -> "Jueves";
            case 5 -> "Viernes";
            case 6 -> "Sábado";
            case 7 -> "Domingo";
            default -> "Día " + dia;
        };
    }

    private static String formatHora(LocalTime hora) {
        return hora != null ? hora.format(HORA_FMT) : "";
    }

    /** Fila aplanada del Excel de horarios: un renglón por cada día del horario. */
    private record FilaHorario(String horario, Integer dia, LocalTime entrada, LocalTime salida) {}

    /**
     * Genera el Excel de todos los horarios registrados. Cada día de cada
     * horario es una fila, con el nombre del horario repetido para que el
     * usuario lea fácilmente "qué días tiene y a qué hora".
     */
    @Transactional(readOnly = true)
    public byte[] exportarExcel() throws IOException {
        List<Horario> horarios = horarioRepository.findAll();

        List<FilaHorario> filas = new ArrayList<>();
        horarios.stream()
                .sorted(Comparator.comparing(Horario::getNombre, String.CASE_INSENSITIVE_ORDER))
                .forEach(h -> h.getDetalles().stream()
                        .sorted(Comparator.comparing(HorarioDetalle::getDia))
                        .forEach(d -> filas.add(new FilaHorario(
                                h.getNombre(), d.getDia(), d.getHoraEntrada(), d.getHoraSalida()))));

        String[] headers = { "Horario", "Día", "Hora de entrada", "Hora de salida" };
        List<Function<FilaHorario, String>> extractores = List.of(
                FilaHorario::horario,
                f -> nombreDia(f.dia()),
                f -> formatHora(f.entrada()),
                f -> formatHora(f.salida())
        );

        return exportExcelService.generar("Horarios", headers, filas, extractores);
    }

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