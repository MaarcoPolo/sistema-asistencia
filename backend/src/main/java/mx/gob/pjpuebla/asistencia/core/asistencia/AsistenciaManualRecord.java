package mx.gob.pjpuebla.asistencia.core.asistencia;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AsistenciaManualRecord(
    Long id,
    Integer usuarioId,
    LocalDate fecha,
    LocalDateTime horaEntrada,
    LocalDateTime horaSalida,
    Boolean esRetardo
) {}