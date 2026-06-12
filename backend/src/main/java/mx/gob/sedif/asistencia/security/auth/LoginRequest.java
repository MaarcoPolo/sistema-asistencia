package mx.gob.sedif.asistencia.security.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// el JSON que el frontend enviará para el login
public record LoginRequest(
        @NotBlank(message = "El número de control es obligatorio")
        @Size(max = 50, message = "El número de control no puede exceder 50 caracteres")
        String numeroControl,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 72, message = "La contraseña no puede exceder 72 caracteres")
        String password
) {}