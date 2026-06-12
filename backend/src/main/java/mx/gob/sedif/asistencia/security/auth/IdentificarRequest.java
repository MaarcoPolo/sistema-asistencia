package mx.gob.sedif.asistencia.security.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// identificar a un usuario sin contraseña
public record IdentificarRequest(
        @NotBlank(message = "El número de control es obligatorio")
        @Size(max = 50, message = "El número de control no puede exceder 50 caracteres")
        String numeroControl
) {}