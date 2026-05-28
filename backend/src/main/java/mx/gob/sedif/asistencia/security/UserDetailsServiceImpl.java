package mx.gob.sedif.asistencia.security;

import lombok.RequiredArgsConstructor;
import mx.gob.sedif.asistencia.core.usuario.Usuario;
import mx.gob.sedif.asistencia.core.usuario.UsuarioRepository;
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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // En nuestro contexto, el 'username' que llega del login es el numero de control
        Usuario usuario = usuarioRepository.findByNumeroControl(username)
                .orElseThrow(() -> new UsernameNotFoundException("No se encontró usuario con número de control: " + username));

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name());

        String password = usuario.getPassword() != null ? usuario.getPassword() : "";
        
        return new User(
                usuario.getNumeroControl(),
                password,
                Collections.singletonList(authority)
        );
    }
}