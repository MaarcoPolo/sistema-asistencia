package mx.gob.sedif.asistencia.security.auth;

/**
 * Subconjunto de datos del usuario enviado al frontend tras el login.
 * No incluye contraseña ni datos sensibles.
 *
 * @param numeroControl Identificador único del usuario.
 * @param nombreCompleto Nombre completo formateado.
 * @param role  Nombre del rol (SUPERADMIN, ADMIN, USER).
 * @param area  Nombre del área principal del usuario.
 * @param requiereCambioPassword Indica si el usuario debe cambiar su contraseña.
 */
public record UserInfoDto(
        String numeroControl,
        String nombreCompleto,
        String role,
        String area,
        Boolean requiereCambioPassword
) {}
