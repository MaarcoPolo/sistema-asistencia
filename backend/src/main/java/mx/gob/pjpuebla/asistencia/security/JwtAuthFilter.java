package mx.gob.pjpuebla.asistencia.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import org.springframework.lang.NonNull;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Extraer el token de la cabecera de la petición
        String token = getTokenFromRequest(request);

        // Validar el token
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            // Obtener la matrícula (username) del token
            String username = jwtTokenProvider.getUsernameFromToken(token);

            // Cargar los detalles del usuario desde la base de datos
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Crear un objeto de autenticación
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            
            // Establecer la autenticación en el contexto de seguridad de Spring
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Extrae solo el token
        }
        return null;
    }
}