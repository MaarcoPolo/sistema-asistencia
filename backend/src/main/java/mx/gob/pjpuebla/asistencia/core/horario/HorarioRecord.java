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
    // Campos derivados para mostrar en la tabla
    String nombreUsuario,
    String nombreArea
) {}