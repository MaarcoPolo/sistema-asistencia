package mx.gob.pjpuebla.asistencia.core.asistencia;

import java.time.LocalDate;
import java.time.LocalDateTime;

//fila en el reporte de asistencias para el admin
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
    String fotoSalida
) {}