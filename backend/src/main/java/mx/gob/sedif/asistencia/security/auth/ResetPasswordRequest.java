package mx.gob.sedif.asistencia.security.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Restablecer la contraseña a la inicial, identificando al usuario por su número de control.
public record ResetPasswordRequest(
        @NotBlank(message = "El número de control es obligatorio")
        @Size(max = 50, message = "El número de control no puede exceder 50 caracteres")
        String numeroControl
) {}
