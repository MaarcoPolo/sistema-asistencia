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
   @Query(value = "SELECT u FROM Usuario u JOIN FETCH u.areaPrincipal " +
           "WHERE (cast(:numeroControl as string) IS NULL OR lower(u.numeroControl) LIKE lower(concat('%', cast(:numeroControl as string), '%'))) " +
           "AND (cast(:nombre as string) IS NULL OR lower(concat(u.nombre, ' ', u.apellidoPaterno, ' ', coalesce(u.apellidoMaterno,''))) LIKE lower(concat('%', cast(:nombre as string), '%'))) " +
           "AND (:areaId IS NULL OR u.areaPrincipal.id = :areaId)",
           countQuery = "SELECT count(u) FROM Usuario u " +
           "WHERE (cast(:numeroControl as string) IS NULL OR lower(u.numeroControl) LIKE lower(concat('%', cast(:numeroControl as string), '%'))) " +
           "AND (cast(:nombre as string) IS NULL OR lower(concat(u.nombre, ' ', u.apellidoPaterno, ' ', coalesce(u.apellidoMaterno,''))) LIKE lower(concat('%', cast(:nombre as string), '%'))) " +
           "AND (:areaId IS NULL OR u.areaPrincipal.id = :areaId)")
    Page<Usuario> findByFiltros(
            @Param("numeroControl") String numeroControl,
            @Param("nombre") String nombre,
            @Param("areaId") Integer areaId,
            Pageable pageable);

    /**
     * Lista paginada de usuarios cuya área principal está dentro de un conjunto de IDs.
     * Usado por administradores de área para ver solo sus empleados asignados.
     */
   @Query(value = "SELECT u FROM Usuario u JOIN FETCH u.areaPrincipal " +
           "WHERE u.areaPrincipal.id IN :areasPermitidas " +
           "AND (cast(:numeroControl as string) IS NULL OR lower(u.numeroControl) LIKE lower(concat('%', cast(:numeroControl as string), '%'))) " +
           "AND (cast(:nombre as string) IS NULL OR lower(concat(u.nombre, ' ', u.apellidoPaterno, ' ', coalesce(u.apellidoMaterno,''))) LIKE lower(concat('%', cast(:nombre as string), '%'))) " +
           "AND (:areaId IS NULL OR u.areaPrincipal.id = :areaId)",
           countQuery = "SELECT count(u) FROM Usuario u " +
           "WHERE u.areaPrincipal.id IN :areasPermitidas " +
           "AND (cast(:numeroControl as string) IS NULL OR lower(u.numeroControl) LIKE lower(concat('%', cast(:numeroControl as string), '%'))) " +
           "AND (cast(:nombre as string) IS NULL OR lower(concat(u.nombre, ' ', u.apellidoPaterno, ' ', coalesce(u.apellidoMaterno,''))) LIKE lower(concat('%', cast(:nombre as string), '%'))) " +
           "AND (:areaId IS NULL OR u.areaPrincipal.id = :areaId)")
    Page<Usuario> findByFiltrosAndAreasPermitidas(
            @Param("numeroControl") String numeroControl,
            @Param("nombre") String nombre,
            @Param("areaId") Integer areaId,
            @Param("areasPermitidas") Set<Integer> areasPermitidas,
            Pageable pageable);

    /**
     * Trae todos los IDs de usuarios en una lista para la carga en lote
     * de los {@link mx.gob.sedif.asistencia.core.horario.HorarioUsuario}.
     * Evita N queries individuales al construir la página de respuesta.
     */
    @Query("SELECT u.id FROM Usuario u WHERE u.id IN :ids")
    List<Integer> findIdsByIdIn(@Param("ids") List<Integer> ids);
}