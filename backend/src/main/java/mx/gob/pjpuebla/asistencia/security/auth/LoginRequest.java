package mx.gob.pjpuebla.asistencia.security.auth;

// Este record modela el JSON que el frontend enviará para el login
public record LoginRequest(String matricula, String password) {}