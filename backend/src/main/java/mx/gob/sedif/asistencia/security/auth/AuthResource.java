package mx.gob.sedif.asistencia.security.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.sedif.asistencia.core.usuario.Usuario;
import mx.gob.sedif.asistencia.core.usuario.UsuarioRepository;
import mx.gob.sedif.asistencia.security.JwtTokenProvider;
import mx.gob.sedif.asistencia.security.UserDetailsServiceImpl;
import mx.gob.sedif.asistencia.util.enums.Rol;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;

/**
 * Servicio de autenticación que gestiona login, identificación rápida y
 * renovación de access tokens mediante refresh token en cookie HttpOnly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthResource {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    // 7 días en segundos para la cookie del refresh token
    private static final int REFRESH_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Autentica un administrador con número de control y contraseña.
     * Emite un access token en el body y un refresh token en cookie HttpOnly.
     *
     * @param loginRequest Credenciales del usuario.
     * @param response     HttpServletResponse para agregar la cookie de refresh.
     * @return JwtResponse con el access token y datos básicos del usuario.
     */
    @Transactional
    public JwtResponse login(LoginRequest loginRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.numeroControl(),
                        loginRequest.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenProvider.createToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication.getName());

        addRefreshCookie(response, refreshToken);

        Usuario usuario = usuarioRepository.findByNumeroControl(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado tras autenticación"));

        String nombreCompleto = buildNombreCompleto(usuario);
        UserInfoDto userInfo = new UserInfoDto(
                usuario.getNumeroControl(),
                nombreCompleto,
                usuario.getRol().name(),
                usuario.getAreaPrincipal().getNombre(),
                usuario.getRequiereCambioPassword()
        );

        return new JwtResponse(accessToken, userInfo);
    }

    /**
     * Identifica a un usuario de tipo USER solo por número de control,
     * sin verificar contraseña (flujo de kiosco de asistencia).
     *
     * @param identificarRequest Número de control del empleado.
     * @param response           HttpServletResponse para la cookie de refresh.
     * @return IdentificarResponse con el access token y datos básicos.
     */
    @Transactional
    public IdentificarResponse identificarUsuario(IdentificarRequest identificarRequest,
                                                  HttpServletResponse response) {
        Usuario usuario = usuarioRepository.findByNumeroControl(identificarRequest.numeroControl())
                .orElseThrow(() -> new RuntimeException("Número de control no encontrado"));

        if (usuario.getRol() != Rol.USER) {
            throw new SecurityException("Acceso denegado. Esta función es solo para usuarios de asistencia.");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                usuario.getNumeroControl(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
        );

        String accessToken = jwtTokenProvider.createToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(usuario.getNumeroControl());

        addRefreshCookie(response, refreshToken);

        return new IdentificarResponse(accessToken, buildNombreCompleto(usuario),
                usuario.getAreaPrincipal().getNombre());
    }

    /**
     * Renueva el access token usando el refresh token almacenado en cookie HttpOnly.
     * Si el refresh token es inválido o no existe, retorna un error para forzar re-login.
     *
     * @param request  HttpServletRequest para leer la cookie de refresh.
     * @param response HttpServletResponse para rotar la cookie de refresh.
     * @return Nuevo access token en un JwtResponse.
     */
    @Transactional
    public JwtResponse refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshCookie(request);

        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new SecurityException("Refresh token inválido o expirado. Inicie sesión nuevamente.");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String newAccessToken = jwtTokenProvider.createToken(auth);
        // Rotar el refresh token para reducir la ventana de reutilización
        String newRefreshToken = jwtTokenProvider.createRefreshToken(username);
        addRefreshCookie(response, newRefreshToken);

        Usuario usuario = usuarioRepository.findByNumeroControl(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        UserInfoDto userInfo = new UserInfoDto(
                usuario.getNumeroControl(),
                buildNombreCompleto(usuario),
                usuario.getRol().name(),
                usuario.getAreaPrincipal().getNombre(),
                usuario.getRequiereCambioPassword()
        );

        return new JwtResponse(newAccessToken, userInfo);
    }

    /**
     * Invalida la cookie de refresh token (logout desde el servidor).
     *
     * @param response HttpServletResponse para eliminar la cookie.
     */
    public void logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // ── Métodos privados ──────────────────────────────────────────────────────

    /**
     * Agrega la cookie HttpOnly con el refresh token a la respuesta.
     * SameSite=Strict evita envíos en requests cross-site.
     */
    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)             // Solo HTTPS en producción
                .path("/api/auth")        // Solo se envía a endpoints de auth
                .maxAge(REFRESH_COOKIE_MAX_AGE)
                .sameSite("Strict")       // Evita envío en requests cross-site (CSRF)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Lee el refresh token de las cookies de la petición.
     *
     * @return El valor de la cookie de refresh, o {@code null} si no existe.
     */
    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Construye el nombre completo del usuario concatenando nombre y apellidos.
     */
    private String buildNombreCompleto(Usuario usuario) {
        String completo = usuario.getNombre() + " " + usuario.getApellidoPaterno() +
                (usuario.getApellidoMaterno() != null ? " " + usuario.getApellidoMaterno() : "");
        return completo.trim();
    }
}
