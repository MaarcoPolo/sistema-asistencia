package mx.gob.sedif.asistencia.core.horario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Integer> {

    @Query("SELECT h FROM Horario h WHERE LOWER(h.nombre) LIKE LOWER(concat('%', :key, '%'))")
    Page<Horario> findAllWithSearch(@Param("key") String key, Pageable pageable);
}