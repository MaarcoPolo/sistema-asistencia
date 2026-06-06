package mx.gob.sedif.asistencia.exception;

/**
 * Constantes de mensajes para todas las respuestas del API.
 *
 * <p>Centralizar los mensajes aquí garantiza coherencia entre backend y frontend,
 * facilita la internacionalización futura y evita mensajes duplicados o inconsistentes.
 * Todos los mensajes están en español, orientados al usuario final.
 */
public final class MessageConstants {

    private MessageConstants() {}

    // ── Autenticación ──────────────────────────────────────────────────────
    public static final String LOGIN_EXITOSO              = "Inicio de sesión exitoso";
    public static final String LOGOUT_EXITOSO             = "Sesión cerrada correctamente";
    public static final String TOKEN_RENOVADO             = "Token renovado correctamente";
    public static final String IDENTIFICACION_EXITOSA     = "Usuario identificado correctamente";
    public static final String CREDENCIALES_INVALIDAS     = "Número de control o contraseña incorrectos";
    public static final String TOKEN_EXPIRADO             = "La sesión ha expirado. Por favor inicie sesión nuevamente";
    public static final String TOKEN_INVALIDO             = "Token de autenticación inválido";
    public static final String REFRESH_TOKEN_INVALIDO     = "Sesión expirada. Por favor inicie sesión nuevamente";
    public static final String ACCESO_DENEGADO            = "No tiene permisos para realizar esta operación";
    public static final String SOLO_ADMINISTRADORES       = "Este recurso es exclusivo para administradores";

    // ── Usuario ────────────────────────────────────────────────────────────
    public static final String USUARIO_CREADO             = "Usuario creado exitosamente";
    public static final String USUARIO_ACTUALIZADO        = "Usuario actualizado correctamente";
    public static final String USUARIO_ELIMINADO          = "Usuario eliminado";
    public static final String USUARIO_NO_ENCONTRADO      = "El usuario solicitado no existe";
    public static final String USUARIO_NO_AUTENTICADO     = "No hay una sesión activa";
    public static final String NUMERO_CONTROL_DUPLICADO   = "El número de control ya está en uso por otro usuario";
    public static final String PASSWORD_RESETEADA         = "Contraseña restablecida al valor por defecto";
    public static final String PASSWORD_CAMBIADA          = "Contraseña actualizada exitosamente";
    public static final String PASSWORD_REQUERIDA_ADMIN   = "La contraseña es obligatoria para crear un administrador";
    public static final String ROL_NO_PERMITIDO           = "No tiene permisos para crear usuarios con este rol";
    public static final String AREA_NO_GESTIONADA         = "No puede operar en un área que no gestiona";
    public static final String EDITAR_SUPERADMIN          = "No tiene permisos para editar a un Superadministrador";
    public static final String ELIMINAR_OTRO_AREA         = "No tiene permisos para eliminar usuarios de esta área";
    public static final String VER_OTRO_AREA              = "No tiene permisos para ver usuarios de esta área";
    public static final String USUARIOS_OBTENIDOS         = "Lista de usuarios obtenida correctamente";
    public static final String USUARIO_OBTENIDO           = "Usuario obtenido correctamente";
    public static final String PERFIL_OBTENIDO            = "Perfil obtenido correctamente";

    // ── Asistencia ─────────────────────────────────────────────────────────
    public static final String ENTRADA_REGISTRADA         = "Entrada registrada exitosamente";
    public static final String SALIDA_REGISTRADA          = "Salida registrada exitosamente";
    public static final String ASISTENCIA_NO_ENCONTRADA   = "No existe un registro de asistencia para esa fecha o ID";
    public static final String ENTRADA_YA_REGISTRADA      = "Ya registraste una entrada para el día de hoy";
    public static final String SALIDA_YA_REGISTRADA       = "Ya has registrado una salida para este turno";
    public static final String SIN_ENTRADA_PREVIA         = "Debes registrar tu entrada antes de poder registrar la salida";
    public static final String IP_NO_AUTORIZADA           = "Intento de registro desde una ubicación (IP) no autorizada";
    public static final String ASISTENCIA_CREADA          = "Registro de asistencia creado exitosamente";
    public static final String ASISTENCIA_ACTUALIZADA     = "Registro de asistencia actualizado correctamente";
    public static final String ASISTENCIA_ELIMINADA       = "Registro de asistencia eliminado";
    public static final String ESTADO_DIARIO_OBTENIDO     = "Estado de asistencia del día obtenido correctamente";
    public static final String REPORTE_OBTENIDO           = "Reporte de asistencias obtenido correctamente";
    public static final String SANCIONES_CALCULADAS       = "Resumen de sanciones calculado correctamente";

    // ── Carga masiva Excel ─────────────────────────────────────────────────
    public static final String EXCEL_PROCESADO            = "Archivo Excel procesado: %d registros, %d errores";
    public static final String EXCEL_FORMATO_INVALIDO     = "El formato del archivo Excel es inválido o está corrupto";
    public static final String EXCEL_SIN_PERMISO          = "No tiene permisos para realizar cargas masivas de asistencia";

    // ── Justificaciones ────────────────────────────────────────────────────
    public static final String JUSTIFICACION_APLICADA     = "Justificación aplicada exitosamente";
    public static final String YA_TIENE_JUSTIFICACION     = "Este registro ya tiene una justificación aplicada";
    public static final String JUSTIFICACION_NO_EXISTE    = "El motivo de justificación seleccionado no existe";
    public static final String OBSERVACION_REQUERIDA      = "Este motivo requiere una observación obligatoria";
    public static final String JUSTIFICACION_AJENA        = "No tienes permiso para justificar una asistencia que no es tuya";
    public static final String JUSTIFICACIONES_OBTENIDAS  = "Catálogo de justificaciones obtenido correctamente";
    public static final String JUSTIFICACION_CREADA       = "Motivo de justificación creado exitosamente";
    public static final String JUSTIFICACION_ACTUALIZADA  = "Motivo de justificación actualizado correctamente";
    public static final String JUSTIFICACION_ELIMINADA    = "Motivo de justificación eliminado";

    // ── Área ───────────────────────────────────────────────────────────────
    public static final String AREA_NO_ENCONTRADA         = "El área solicitada no existe";
    public static final String AREAS_OBTENIDAS            = "Lista de áreas obtenida correctamente";
    public static final String AREA_OBTENIDA              = "Área obtenida correctamente";
    public static final String AREA_CREADA                = "Área creada exitosamente";
    public static final String AREA_ACTUALIZADA           = "Área actualizada correctamente";
    public static final String AREA_ELIMINADA             = "Área eliminada";

    // ── Horario ────────────────────────────────────────────────────────────
    public static final String HORARIO_NO_ENCONTRADO      = "El horario solicitado no existe";
    public static final String HORARIOS_OBTENIDOS         = "Lista de horarios obtenida correctamente";
    public static final String HORARIO_CREADO             = "Horario creado exitosamente";
    public static final String HORARIO_ACTUALIZADO        = "Horario actualizado correctamente";
    public static final String HORARIO_ELIMINADO          = "Horario eliminado";
    public static final String EXCEPCION_CREADA           = "Excepción de horario registrada exitosamente";
    public static final String EXCEPCION_ELIMINADA        = "Excepción de horario eliminada";

    // ── Errores generales ──────────────────────────────────────────────────
    public static final String ERROR_INTERNO              = "Error interno del servidor. Contacte al administrador";
    public static final String OPERACION_COMPLETADA       = "Operación completada exitosamente";
}
