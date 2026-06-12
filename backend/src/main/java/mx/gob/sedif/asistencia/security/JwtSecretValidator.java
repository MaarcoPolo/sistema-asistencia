package mx.gob.sedif.asistencia.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Valida, solo en el perfil de producción, que el secreto JWT provenga de una
 * variable de entorno fuerte y no del valor de respaldo de desarrollo (ID-015).
 *
 * <p>Si el secreto está vacío, es demasiado corto o coincide con el fallback
 * conocido, la aplicación falla al arrancar en lugar de operar con una clave
 * insegura y predecible.
 */
@Component
@Profile("prod")
public class JwtSecretValidator {

    /** Fragmento del valor de respaldo definido en application.properties. */
    private static final String FALLBACK_MARKER = "esta-es-una-clave-secreta";

    /** Mínimo de bytes para una clave HMAC-SHA256 segura. */
    private static final int MIN_SECRET_BYTES = 32;

    private final String secret;

    public JwtSecretValidator(@Value("${app.jwt.secret}") String secret) {
        this.secret = secret;
    }

    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "FATAL: app.jwt.secret no está configurado. Defina la variable de entorno JWT_SECRET en producción.");
        }
        if (secret.contains(FALLBACK_MARKER)) {
            throw new IllegalStateException(
                "FATAL: se está usando el secreto JWT de desarrollo en producción. Defina JWT_SECRET.");
        }
        if (secret.getBytes().length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "FATAL: app.jwt.secret es demasiado corto (mínimo " + MIN_SECRET_BYTES + " bytes).");
        }
    }
}
