package mx.gob.pjpuebla.asistencia.core.area;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mx.gob.pjpuebla.asistencia.util.enums.Estado;

import java.util.List;
import java.util.Set;

@Repository
public interface AreaRepository extends JpaRepository<Area, Integer> {

    Page<Area> findByNombreContainingIgnoreCaseAndEstatusNot(String nombre, Estado estatus, Pageable pageable);

    List<Area> findByEstatus(Estado estatus);
    
    @Query("SELECT a FROM Area a WHERE LOWER(a.nombre) LIKE LOWER(concat('%', :key, '%')) AND a.id IN :areaIds AND a.estatus <> mx.gob.pjpuebla.asistencia.util.enums.Estado.DELETED")
    Page<Area> findByNombreAndIds(@Param("key") String key, @Param("areaIds") Set<Integer> areaIds, Pageable pageable);
}