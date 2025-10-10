package mx.gob.pjpuebla.asistencia.core.horario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; 
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Integer>, JpaSpecificationExecutor<Horario> {

    @Query("""
        SELECT h FROM Horario h WHERE h.usuario.id = :usuarioId
        OR (h.area.id = :areaId AND h.usuario.id IS NULL)
        OR (h.area.id IS NULL AND h.usuario.id IS NULL)
        ORDER BY h.usuario.id DESC NULLS LAST, h.area.id DESC NULLS LAST
        """)
    Optional<Horario> findBestMatchForUsuario(Integer usuarioId, Integer areaId);
}