package mx.gob.sedif.asistencia.security.auth;

// el JSON que el frontend enviará para el login
public record LoginRequest(String numeroControl, String password) {}