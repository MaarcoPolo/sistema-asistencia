package mx.gob.pjpuebla.asistencia.security;

import lombok.RequiredArgsConstructor;
import mx.gob.pjpuebla.asistencia.core.usuario.Usuario;
import mx.gob.pjpuebla.asistencia.core.usuario.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String matricula) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByMatricula(matricula)
                .orElseThrow(() -> new UsernameNotFoundException("No se encontró usuario con matrícula: " + matricula));

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name());

        // --- CORRECCIÓN ---
        // Si el usuario no tiene contraseña (es un rol USER), pasamos una cadena vacía.
        String password = usuario.getPassword() != null ? usuario.getPassword() : "";
        
        return new User(
                usuario.getMatricula(),
                password, // Usamos la nueva variable segura
                Collections.singletonList(authority)
        );
    }
}