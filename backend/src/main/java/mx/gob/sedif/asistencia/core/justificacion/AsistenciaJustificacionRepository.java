package mx.gob.sedif.asistencia.core.justificacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AsistenciaJustificacionRepository extends JpaRepository<AsistenciaJustificacion, Long> {
}