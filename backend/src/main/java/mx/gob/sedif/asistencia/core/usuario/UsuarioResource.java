package mx.gob.sedif.asistencia.core.usuario;

import lombok.RequiredArgsConstructor;
import mx.gob.sedif.asistencia.exception.ApiResponse;
import mx.gob.sedif.asistencia.exception.MessageConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller REST para la gestión de usuarios del sistema.
 *
 * <p>Códigos HTTP:
 * <ul>
 *   <li>GET    → 200 OK</li>
 *   <li>POST   → 201 Created</li>
 *   <li>PUT    → 200 OK</li>
 *   <li>DELETE → 204 No Content</li>
 *   <li>Errores → delegados a {@link mx.gob.sedif.asistencia.exception.GlobalExceptionHandler}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/core/usuario")
@RequiredArgsConstructor
public class UsuarioResource {

    private final UsuarioService usuarioService;

    /**
     * Lista paginada de usuarios filtrados por nombre.
     * SUPERADMIN ve todos; ADMIN solo ve usuarios de sus áreas.
     * HTTP 200 con {@code Page<UsuarioRecord>} dentro del wrapper.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<UsuarioRecord>>> getAll(
            @RequestParam(required = false, defaultValue = "") String key,
            @RequestParam(required = false) String numeroControl,
            @RequestParam(required = false) Integer areaId,
            @PageableDefault(size = 25, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<UsuarioRecord> resultado = usuarioService.getAll(key, numeroControl, areaId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.USUARIOS_OBTENIDOS, resultado));
    }

    /**
     * Retorna un usuario por ID.
     * HTTP 200, o 403/404 según el caso de error.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioRecord>> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.ok(MessageConstants.USUARIO_OBTENIDO, usuarioService.findById(id)));
    }

    /**
     * Crea un nuevo usuario.
     * HTTP 201 Created con los datos del usuario recién creado.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioRecord>> create(@RequestBody UsuarioRecord usuarioRecord) {
        UsuarioRecord creado = usuarioService.create(usuarioRecord);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MessageConstants.USUARIO_CREADO, creado));
    }

    /**
     * Actualiza los datos de un usuario existente.
     * HTTP 200 con los datos actualizados.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioRecord>> save(
            @PathVariable Integer id,
            @RequestBody UsuarioRecord usuarioRecord
    ) {
        if (!id.equals(usuarioRecord.id())) {
            throw new IllegalArgumentException("El ID del path y del body no coinciden.");
        }
        UsuarioRecord actualizado = usuarioService.save(usuarioRecord);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.USUARIO_ACTUALIZADO, actualizado));
    }

    /**
     * Aplica soft-delete al usuario (estatus = DELETED).
     * HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteById(@PathVariable Integer id) {
        usuarioService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.noContent(MessageConstants.USUARIO_ELIMINADO));
    }

    /**
     * Retorna el perfil del usuario autenticado.
     * HTTP 200 con los datos del usuario en sesión.
     */
    @GetMapping("/mi-perfil")
    @PreAuthorize("hasAnyRole('USER', 'SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioRecord>> getMiPerfil(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(MessageConstants.PERFIL_OBTENIDO,
                        usuarioService.findByNumeroControl(userDetails.getUsername())));
    }

    /**
     * Restablece la contraseña del usuario al valor predeterminado.
     * HTTP 200.
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Integer id) {
        usuarioService.resetPassword(id);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.PASSWORD_RESETEADA));
    }

    /**
     * Permite al usuario autenticado cambiar su propia contraseña.
     * HTTP 200.
     */
    @PostMapping("/mi-contrasena")
    @PreAuthorize("hasAnyRole('USER', 'SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cambiarMiContrasena(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String nuevaContrasena = request.get("nuevaContrasena");
        String contrasenaActual = request.get("contrasenaActual");
        if (nuevaContrasena == null || nuevaContrasena.isBlank()) {
            throw new IllegalArgumentException("La nueva contraseña no puede estar vacía.");
        }
        if (nuevaContrasena.length() < 8) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres.");
        }
        usuarioService.cambiarMiContrasena(userDetails.getUsername(), contrasenaActual, nuevaContrasena);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.PASSWORD_CAMBIADA));
    }
}
