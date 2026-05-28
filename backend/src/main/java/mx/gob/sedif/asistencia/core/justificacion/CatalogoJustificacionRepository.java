package mx.gob.sedif.asistencia.core.justificacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogoJustificacionRepository extends JpaRepository<CatalogoJustificacion, Integer> {
    
    @Query("SELECT c FROM CatalogoJustificacion c WHERE LOWER(c.nombre) LIKE LOWER(concat('%', :key, '%')) OR LOWER(c.clave) LIKE LOWER(concat('%', :key, '%'))")
    Page<CatalogoJustificacion> findAllWithSearch(@Param("key") String key, Pageable pageable);
}