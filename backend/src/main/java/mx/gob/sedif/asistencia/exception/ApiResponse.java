package mx.gob.sedif.asistencia.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Envoltorio estándar para todas las respuestas del API REST.
 *
 * <p>Todos los endpoints retornan esta estructura para que el frontend pueda
 * manejar éxitos y errores de forma uniforme sin inspeccionar el código HTTP.
 *
 * <pre>
 * {
 *   "success": true,
 *   "code": 200,
 *   "message": "Operación completada",
 *   "data": { ... },
 *   "fieldErrors": null,
 *   "timestamp": "2026-06-05T10:30:00"
 * }
 * </pre>
 *
 * @param <T> Tipo del payload de datos; {@code null} en respuestas de error o 204.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int code,
        String message,
        T data,
        List<FieldError> fieldErrors,
        LocalDateTime timestamp
) {

    // ── Factories de éxito ─────────────────────────────────────────────────

    /** 200 OK con datos y mensaje. */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, 200, message, data, null, LocalDateTime.now());
    }

    /** 200 OK sin datos (para operaciones que no retornan payload). */
    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, 200, message, null, null, LocalDateTime.now());
    }

    /** 201 Created con datos recién creados. */
    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(true, 201, message, data, null, LocalDateTime.now());
    }

    /** 204 No Content para operaciones de eliminación exitosa. */
    public static <T> ApiResponse<T> noContent(String message) {
        return new ApiResponse<>(true, 204, message, null, null, LocalDateTime.now());
    }

    // ── Factories de error ─────────────────────────────────────────────────

    /** Respuesta de error genérica con código HTTP explícito. */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(false, code, message, null, null, LocalDateTime.now());
    }

    /**
     * 400 Bad Request con errores de validación por campo.
     *
     * @param fieldErrors Lista de errores por campo para mostrarse en formularios.
     */
    public static <T> ApiResponse<T> validationError(List<FieldError> fieldErrors) {
        return new ApiResponse<>(false, 400, "Error de validación en los datos enviados.",
                null, fieldErrors, LocalDateTime.now());
    }

    // ── Tipo interno para errores de campo ────────────────────────────────

    /**
     * Error asociado a un campo específico del formulario.
     *
     * @param field   Nombre del campo (e.g. "numeroControl").
     * @param message Descripción del error (e.g. "no debe estar vacío").
     */
    public record FieldError(String field, String message) {}
}
