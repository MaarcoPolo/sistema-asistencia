package mx.gob.sedif.asistencia.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Proveedor de tokens JWT para autenticación sin estado (stateless).
 *
 * <p>Genera dos tipos de tokens:
 * <ul>
 *   <li><b>Access token</b>: corta duración (1 h por defecto), enviado en header Authorization.</li>
 *   <li><b>Refresh token</b>: larga duración (7 d por defecto), enviado solo en cookie HttpOnly.</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessValidityMs;
    private final long refreshValidityMs;

    /**
     * @param secretKey        Clave HMAC leída de la propiedad {@code app.jwt.secret}.
     * @param accessValidityMs Duración del access token en milisegundos.
     * @param refreshValidityMs Duración del refresh token en milisegundos.
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secretKey,
            @Value("${app.jwt.expiration-ms}") long accessValidityMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshValidityMs
    ) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.accessValidityMs = accessValidityMs;
        this.refreshValidityMs = refreshValidityMs;
    }

    /**
     * Genera un access token JWT firmado con los roles del usuario autenticado.
     *
     * @param authentication Objeto de autenticación de Spring Security.
     * @return String del JWT compacto.
     */
    public String createToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessValidityMs);
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .claim("roles", roles)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    /**
     * Genera un refresh token JWT de larga duración para un usuario específico.
     * No incluye roles; solo sirve para obtener un nuevo access token.
     *
     * @param username Número de control del usuario.
     * @return String del JWT de refresh compacto.
     */
    public String createRefreshToken(String username) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshValidityMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .claim("type", "refresh")
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    /**
     * Extrae el número de control (subject) del payload del token.
     *
     * @param token JWT compacto (access o refresh).
     * @return Número de control del usuario.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Valida la firma y la expiración del token.
     *
     * @param token JWT a validar.
     * @return {@code true} si el token es válido y no ha expirado.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Token inválido o expirado: {}", e.getMessage());
            return false;
        }
    }
}
