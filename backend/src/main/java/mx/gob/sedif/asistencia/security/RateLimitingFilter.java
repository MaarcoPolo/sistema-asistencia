package mx.gob.sedif.asistencia.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita la tasa de peticiones a los endpoints de autenticación para frenar
 * fuerza bruta de contraseñas y enumeración de números de control (ID-013).
 *
 * <p>Aplica un cubo de tokens por (IP + ruta): 5 intentos por minuto. Detrás de
 * un proxy, requiere {@code server.forward-headers-strategy} configurado para que
 * {@code getRemoteAddr()} refleje la IP real del cliente (ya activado en prod).
 *
 * <p>El mapa de cubos es en memoria y por instancia; suficiente para un despliegue
 * single-instance. Para múltiples instancias migrar a un backend distribuido.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Solo limita login e identificar; el resto de la app no se ve afectado.
        return !(path.endsWith("/api/auth/login") || path.endsWith("/api/auth/identificar"));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String key = request.getRequestURI() + ":" + request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"success\":false,\"message\":\"Demasiados intentos. Intente de nuevo en un minuto.\"}");
        }
    }
}
