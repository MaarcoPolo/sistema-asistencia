package mx.gob.sedif.asistencia.core.justificacion;

public record CatalogoJustificacionRecord(
    Integer id,
    String clave,
    String nombre,
    Boolean requiereObservacion
) {}