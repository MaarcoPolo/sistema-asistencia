package mx.gob.sedif.asistencia.core.asistencia;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.sedif.asistencia.core.usuario.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Job programado del cierre nocturno de incidencias.
 *
 * <p>Vive en un bean separado de {@link AsistenciaService} a propósito: así la
 * llamada a {@link AsistenciaService#procesarLote} cruza el proxy de Spring y la
 * anotación {@code @Transactional} de cada lote se aplica de verdad (con
 * self-invocation dentro del mismo bean, la transacción por lote no se activaba —
 * PERF-015).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsistenciaNocturnaJob {

    private final AsistenciaService asistenciaService;

    /**
     * Se ejecuta a las 23:50:00 todos los días para cerrar las incidencias del día.
     * Itera los usuarios por páginas; cada lote se procesa en su propia transacción.
     */
    @Scheduled(cron = "0 50 23 * * ?")
    public void ejecutar() {
        LocalDate hoy = LocalDate.now();
        int pageNumber = 0;
        int totalProcesados = 0;

        log.info("Iniciando cierre de incidencias nocturno para la fecha: {}", hoy);

        Page<Usuario> pagina;
        do {
            pagina = asistenciaService.obtenerPaginaUsuarios(pageNumber);
            // Llamada cross-bean: el @Transactional de procesarLote sí aplica aquí.
            asistenciaService.procesarLote(pagina.getContent(), hoy);
            totalProcesados += pagina.getNumberOfElements();
            pageNumber++;
        } while (pagina.hasNext());

        log.info("Cierre nocturno completado: {} usuarios procesados para la fecha {}", totalProcesados, hoy);
    }
}
