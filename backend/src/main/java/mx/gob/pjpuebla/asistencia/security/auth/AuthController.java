package mx.gob.pjpuebla.asistencia.security.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthResource authService; 

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/identificar")
    public ResponseEntity<IdentificarResponse> identificar(@RequestBody IdentificarRequest request) {
        return ResponseEntity.ok(authService.identificarUsuario(request));
    }
}