package mx.gob.pjpuebla.asistencia.core.area;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import mx.gob.pjpuebla.asistencia.util.enums.Estado;

import java.util.List;

@Repository
public interface AreaRepository extends JpaRepository<Area, Integer> {

    Page<Area> findByNombreContainingIgnoreCaseAndEstatusNot(String nombre, Estado estatus, Pageable pageable);

    List<Area> findByEstatus(Estado estatus);
}