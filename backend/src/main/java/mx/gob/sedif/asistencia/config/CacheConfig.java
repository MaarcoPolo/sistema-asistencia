package mx.gob.sedif.asistencia.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Habilita el soporte de caché de Spring. El proveedor (Caffeine) y su política
 * de expiración se configuran por propiedades en application.properties
 * ({@code spring.cache.type=caffeine}).
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
