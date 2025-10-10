package mx.gob.pjpuebla.asistencia.security.auth;

import lombok.RequiredArgsConstructor;
import mx.gob.pjpuebla.asistencia.core.usuario.Usuario;
import mx.gob.pjpuebla.asistencia.core.usuario.UsuarioRepository;
import mx.gob.pjpuebla.asistencia.security.JwtTokenProvider;
import mx.gob.pjpuebla.asistencia.util.enums.Rol;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthResource {

        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider jwtTokenProvider;
        private final UsuarioRepository usuarioRepository; // Inyectar el repositorio

        @PostMapping("/login")
        public JwtResponse login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.matricula(),
                        loginRequest.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.createToken(authentication);
        return new JwtResponse(token);
        }

        // --- MÉTODO NUEVO PARA EL ROL USER ---
        @PostMapping("/identificar")
        public IdentificarResponse identificarUsuario(@RequestBody IdentificarRequest identificarRequest) {
        // Buscamos al usuario por su matrícula
        Usuario usuario = usuarioRepository.findByMatricula(identificarRequest.matricula())
                .orElseThrow(() -> new RuntimeException("Matrícula no encontrada"));

        // Validamos que sea un usuario regular
        if (usuario.getRol() != Rol.USER) {
                throw new SecurityException("Acceso denegado. Esta función es solo para usuarios de asistencia.");
        }

        // Creamos un objeto de autenticación para este usuario (sin validar contraseña)
        // Esto es necesario para que el JwtTokenProvider pueda generar un token
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                usuario.getMatricula(),
                null, // La contraseña es nula
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
        );

        // Generamos el token de corta duración
        String token = jwtTokenProvider.createToken(authentication);

        // Construimos el nombre completo
        String nombreCompleto = usuario.getNombre() + " " + usuario.getApellidoPaterno() +
                (usuario.getApellidoMaterno() != null ? " " + usuario.getApellidoMaterno() : "");

        return new IdentificarResponse(token, nombreCompleto.trim(), usuario.getAreaPrincipal().getNombre());
        }
}