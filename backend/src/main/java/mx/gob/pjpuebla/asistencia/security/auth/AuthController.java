package mx.gob.pjpuebla.asistencia.security.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // AuthResource está actuando como nuestro servicio, lo renombramos aquí para mayor claridad.
    private final AuthResource authService; 

    @PostMapping("/login")
    // 1. CORRECCIÓN: El método ahora recibe un 'LoginRequest', que es lo que el servicio espera.
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/identificar")
    public ResponseEntity<IdentificarResponse> identificar(@RequestBody IdentificarRequest request) {
        // 2. CORRECCIÓN: Se usa 'request.matricula()' en lugar de 'request.getMatricula()' porque es un record.
        return ResponseEntity.ok(authService.identificarUsuario(request));
    }
}