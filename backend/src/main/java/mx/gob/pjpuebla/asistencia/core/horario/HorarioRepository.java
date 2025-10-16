package mx.gob.pjpuebla.asistencia.core.horario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Set;
import java.util.List;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Integer> {

    @Query("""
        SELECT h FROM Horario h WHERE h.usuario.id = :usuarioId
        OR (h.area.id = :areaId AND h.usuario IS NULL)
        OR (h.area IS NULL AND h.usuario IS NULL)
        ORDER BY h.usuario.id DESC NULLS LAST, h.area.id DESC NULLS LAST
        """)
    List<Horario> findBestMatchForUsuario(@Param("usuarioId") Integer usuarioId, @Param("areaId") Integer areaId);

    // Consulta para SUPERADMIN con búsqueda
    @Query("SELECT h FROM Horario h LEFT JOIN h.usuario u LEFT JOIN h.area a WHERE LOWER(h.nombre) LIKE LOWER(concat('%', :key, '%')) OR LOWER(u.nombre) LIKE LOWER(concat('%', :key, '%')) OR LOWER(a.nombre) LIKE LOWER(concat('%', :key, '%'))")
    Page<Horario> findAllWithSearch(@Param("key") String key, Pageable pageable);
    
    // Consulta para ADMIN con búsqueda y filtro por áreas
    @Query("""
        SELECT h FROM Horario h LEFT JOIN h.usuario u LEFT JOIN h.area a 
        WHERE (a.id IN :areaIds OR u.areaPrincipal.id IN :areaIds OR (h.area IS NULL AND h.usuario IS NULL)) 
        AND (LOWER(h.nombre) LIKE LOWER(concat('%', :key, '%')) OR LOWER(u.nombre) LIKE LOWER(concat('%', :key, '%')) OR LOWER(a.nombre) LIKE LOWER(concat('%', :key, '%')))
        """)
    Page<Horario> findAllInAreasWithSearch(@Param("key") String key, @Param("areaIds") Set<Integer> areaIds, Pageable pageable);
}