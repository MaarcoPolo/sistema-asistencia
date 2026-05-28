package mx.gob.sedif.asistencia.core.horario;

import java.time.LocalTime;

public record HorarioDetalleRecord(
    Integer id,
    Integer dia,
    LocalTime horaEntrada,
    LocalTime horaSalida,
    Integer toleranciaMinutos
) {}