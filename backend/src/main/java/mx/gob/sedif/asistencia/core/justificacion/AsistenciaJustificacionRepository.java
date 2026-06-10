package mx.gob.sedif.asistencia.core.justificacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AsistenciaJustificacionRepository extends JpaRepository<AsistenciaJustificacion, Long> {

    /**
     * Recupera la justificación asociada a una asistencia consultando directamente
     * por el id de la asistencia, sin depender de la relación lazy inversa
     * {@code Asistencia.justificacionAplicada} (que puede traer datos cacheados o
     * inconsistentes si hubiera filas duplicadas de pruebas previas).
     *
     * @param asistenciaId Id de la asistencia.
     * @return La justificación si existe, vacío en caso contrario.
     */
    Optional<AsistenciaJustificacion> findByAsistenciaId(Long asistenciaId);
}