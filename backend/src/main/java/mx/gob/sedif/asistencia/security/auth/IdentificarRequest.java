package mx.gob.sedif.asistencia.security.auth;

// identificar a un usuario sin contraseña
public record IdentificarRequest(String numeroControl) {}