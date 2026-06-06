package mx.gob.sedif.asistencia.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manejador centralizado de excepciones para todos los controllers REST.
 *
 * <p>Garantiza que el cliente siempre reciba un {@link ApiResponse} con estructura
 * uniforme, sin exponer stack traces ni nombres de clases internas.
 * Los detalles técnicos se registran en el log del servidor.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Captura errores de validación de Bean Validation (@Valid en @RequestBody).
     * Retorna 400 con la lista de campos inválidos y el mensaje de cada uno.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());

        log.warn("Error de validación: {} campos inválidos", fieldErrors.size());
        return ResponseEntity.badRequest().body(ApiResponse.validationError(fieldErrors));
    }

    /**
     * Maneja errores de negocio esperados (entidad no encontrada, duplicados, etc.).
     * Retorna 400 con el mensaje de la excepción.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex) {
        log.warn("Error de negocio: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, ex.getMessage()));
    }

    /**
     * Maneja violaciones de estado (ej. doble registro de entrada).
     * Retorna 409 Conflict.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("Estado inválido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, ex.getMessage()));
    }

    /**
     * Maneja argumentos inválidos en la petición.
     * Retorna 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, ex.getMessage()));
    }

    /**
     * Maneja intentos de acceso no autorizado a recursos de otro usuario o área.
     * Retorna 403 con mensaje genérico para no revelar la estructura interna.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurity(SecurityException ex) {
        log.warn("Acceso denegado (SecurityException): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, MessageConstants.ACCESO_DENEGADO));
    }

    /**
     * Maneja el rechazo de Spring Security por rol insuficiente (@PreAuthorize).
     * Retorna 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acceso denegado (@PreAuthorize): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, MessageConstants.ACCESO_DENEGADO));
    }

    /**
     * Captura cualquier excepción no prevista para evitar que Spring devuelva
     * un stack trace al cliente. Retorna 500 con mensaje genérico.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Error inesperado en el servidor", ex);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, MessageConstants.ERROR_INTERNO));
    }
}
