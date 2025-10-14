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
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service 
@RequiredArgsConstructor
public class AuthResource {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;

    // Ya no es un endpoint, sino un método de servicio público
    public JwtResponse login(LoginRequest loginRequest) {
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

    // Ya no es un endpoint, sino un método de servicio público
    public IdentificarResponse identificarUsuario(IdentificarRequest identificarRequest) {
        Usuario usuario = usuarioRepository.findByMatricula(identificarRequest.matricula())
                .orElseThrow(() -> new RuntimeException("Matrícula no encontrada"));

        if (usuario.getRol() != Rol.USER) {
            throw new SecurityException("Acceso denegado. Esta función es solo para usuarios de asistencia.");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                usuario.getMatricula(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
        );

        String token = jwtTokenProvider.createToken(authentication);

        String nombreCompleto = usuario.getNombre() + " " + usuario.getApellidoPaterno() +
                (usuario.getApellidoMaterno() != null ? " " + usuario.getApellidoMaterno() : "");

        return new IdentificarResponse(token, nombreCompleto.trim(), usuario.getAreaPrincipal().getNombre());
    }
}