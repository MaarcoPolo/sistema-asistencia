package mx.gob.pjpuebla.asistencia.core.asistencia;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AsistenciaReporteRecord(
    Long idAsistencia,
    LocalDate fecha,
    LocalDateTime horaEntrada,
    LocalDateTime horaSalida,
    boolean esRetardo,
    Integer usuarioId,
    String usuarioMatricula,
    String usuarioNombreCompleto,
    Integer areaId,
    String areaNombre,
    String fotoEntrada,
    String fotoSalida,
    String ipRegistro
) {}