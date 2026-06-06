package mx.gob.sedif.asistencia.security.auth;

/**
 * Respuesta del endpoint de login para administradores.
 * Solo contiene el access token; el refresh token viaja en cookie HttpOnly.
 *
 * @param token Access token JWT de corta duración.
 * @param user  Datos básicos del usuario para el estado del frontend.
 */
public record JwtResponse(String token, UserInfoDto user) {}