package mx.gob.sedif.asistencia.core.usuario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repositorio JPA para la entidad {@link Usuario}.
 *
 * <p>Las queries de paginación usan {@code JOIN FETCH} sobre {@code areaPrincipal}
 * para evitar el problema N+1 al mapear cada usuario a su DTO de respuesta.
 * Las relaciones {@code areasGestionadas} se cargan en una consulta separada
 * (patrón "select in batch") para no generar un producto cartesiano en la paginación.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /** Busca un usuario por su número de control único. */
    Optional<Usuario> findByNumeroControl(String numeroControl);

    /**
     * Lista paginada de todos los usuarios filtrados por nombre completo.
     * El {@code JOIN FETCH areaPrincipal} evita una query adicional por fila.
     */
    @Query("SELECT u FROM Usuario u JOIN FETCH u.areaPrincipal " +
           "WHERE lower(concat(u.nombre, ' ', u.apellidoPaterno, ' ', " +
           "coalesce(u.apellidoMaterno,''))) LIKE lower(concat('%', :nombre, '%'))")
    Page<Usuario> findByNombreCompletoContaining(@Param("nombre") String nombre, Pageable pageable);

    /**
     * Lista paginada de usuarios cuya área principal está dentro de un conjunto de IDs.
     * Usado por administradores de área para ver solo sus empleados asignados.
     */
    @Query("SELECT u FROM Usuario u JOIN FETCH u.areaPrincipal " +
           "WHERE u.areaPrincipal.id IN :areaIds " +
           "AND lower(concat(u.nombre, ' ', u.apellidoPaterno, ' ', " +
           "coalesce(u.apellidoMaterno,''))) LIKE lower(concat('%', :nombre, '%'))")
    Page<Usuario> findByNombreCompletoInAreaIds(
            @Param("nombre") String nombre,
            @Param("areaIds") Set<Integer> areaIds,
            Pageable pageable);

    /**
     * Trae todos los IDs de usuarios en una lista para la carga en lote
     * de los {@link mx.gob.sedif.asistencia.core.horario.HorarioUsuario}.
     * Evita N queries individuales al construir la página de respuesta.
     */
    @Query("SELECT u.id FROM Usuario u WHERE u.id IN :ids")
    List<Integer> findIdsByIdIn(@Param("ids") List<Integer> ids);
}