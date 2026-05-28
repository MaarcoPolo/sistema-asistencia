package mx.gob.sedif.asistencia.core.horario;

import java.util.List;

public record HorarioRecord(
    Integer id,
    String nombre,
    Boolean cruceMedianoche,
    Integer tipoCiclo,
    List<HorarioDetalleRecord> detalles
) {}