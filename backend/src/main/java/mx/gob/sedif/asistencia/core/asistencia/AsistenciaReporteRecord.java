package mx.gob.sedif.asistencia.core.asistencia;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AsistenciaReporteRecord(
    Long idAsistencia,
    LocalDate fecha,
    LocalDateTime horaEntrada,
    LocalDateTime horaSalida,
    Integer estatusIncidencia, 
    Integer usuarioId,
    String usuarioNumeroControl, 
    String usuarioNombreCompleto,
    Integer areaId,
    String areaNombre,
    String fotoEntrada,
    String fotoSalida,
    String ipRegistro,
    String motivoJustificacion,
    String estatusJustificacion
) {}