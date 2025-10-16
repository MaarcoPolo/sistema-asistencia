package mx.gob.pjpuebla.asistencia.core.horario;

import java.time.LocalTime;

public record HorarioRecord(
    Integer id,
    String nombre,
    LocalTime horaEntrada,
    LocalTime horaSalida,
    Integer toleranciaMinutos,
    Integer idUsuario,
    Integer idArea,
    String nombreUsuario,
    String nombreArea
) {}