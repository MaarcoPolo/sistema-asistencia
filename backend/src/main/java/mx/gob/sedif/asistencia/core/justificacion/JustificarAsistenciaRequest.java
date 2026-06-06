package mx.gob.sedif.asistencia.core.justificacion;

public record JustificarAsistenciaRequest(
    Integer justificacionId,
    String observacion
) {}