package mx.gob.pjpuebla.asistencia.security.auth;

// Modela la petición para identificar a un usuario sin contraseña
public record IdentificarRequest(String matricula) {}