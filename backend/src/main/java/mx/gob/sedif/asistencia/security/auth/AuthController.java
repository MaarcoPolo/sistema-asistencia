package mx.gob.sedif.asistencia.security.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para los flujos de autenticación.
 *
 * <ul>
 *   <li>{@code POST /api/auth/login} — Login de administradores con contraseña.</li>
 *   <li>{@code POST /api/auth/identificar} — Identificación rápida para el kiosco (sin contraseña).</li>
 *   <li>{@code POST /api/auth/refresh} — Renueva el access token usando la cookie de refresh.</li>
 *   <li>{@code POST /api/auth/logout} — Invalida la cookie de refresh en el servidor.</li>
 * </ul>
 *
 * Todos los endpoints de este controller son públicos (configurado en SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthResource authService;

    /**
     * Autentica al usuario con número de control y contraseña.
     * Retorna el access token en body y emite el refresh token en cookie HttpOnly.
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(loginRequest, response));
    }

    /**
     * Identifica a un empleado solo con su número de control (flujo de kiosco).
     * No valida contraseña; la seguridad recae en la restricción por IP del área.
     */
    @PostMapping("/identificar")
    public ResponseEntity<IdentificarResponse> identificar(
            @Valid @RequestBody IdentificarRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(authService.identificarUsuario(request, response));
    }

    /**
     * Renueva el access token leyendo el refresh token de la cookie HttpOnly.
     * Si la cookie no existe o está expirada retorna 403 para forzar re-login.
     */
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(authService.refreshAccessToken(request, response));
    }

    /**
     * Invalida la cookie de refresh token en el servidor.
     * El frontend debe también eliminar el access token de su almacenamiento local.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.noContent().build();
    }

    /**
     * Restablece la contraseña del usuario a la inicial, identificándolo por el
     * número de control que él mismo reescribe (flujo "olvidé mi contraseña").
     * Si el número de control no existe retorna 400 con el mensaje de verificación.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPasswordPorNumeroControl(request);
        return ResponseEntity.noContent().build();
    }
}
