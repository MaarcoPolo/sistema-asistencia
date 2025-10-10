package mx.gob.pjpuebla.asistencia.security;

import mx.gob.pjpuebla.asistencia.core.usuario.Usuario;
import mx.gob.pjpuebla.asistencia.core.usuario.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Component
public class SecurityUtil {

    private final UsuarioRepository usuarioRepository;

    public SecurityUtil(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Obtiene el objeto Usuario completo de la base de datos para el usuario actualmente autenticado.
     * @return Un Optional que contiene el Usuario si se encuentra.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Usuario> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }
        String matricula = ((User) authentication.getPrincipal()).getUsername();
        return usuarioRepository.findByMatricula(matricula);
    }
}