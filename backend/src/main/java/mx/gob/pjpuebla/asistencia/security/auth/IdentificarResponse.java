package mx.gob.pjpuebla.asistencia.security.auth;

// Modela la respuesta con los datos del usuario y su token temporal
public record IdentificarResponse(
    String token,
    String nombreCompleto,
    String area
) {}