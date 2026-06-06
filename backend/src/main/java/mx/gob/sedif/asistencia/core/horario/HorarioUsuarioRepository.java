package mx.gob.sedif.asistencia.core.horario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la relación {@link HorarioUsuario} (tabla intermedia).
 */
@Repository
public interface HorarioUsuarioRepository extends JpaRepository<HorarioUsuario, HorarioUsuarioId> {

    /** Busca la asignación de horario de un usuario específico por su ID. */
    @Query("SELECT hu FROM HorarioUsuario hu JOIN FETCH hu.horario WHERE hu.usuario.id = :usuarioId")
    Optional<HorarioUsuario> findByUsuarioId(@Param("usuarioId") Integer usuarioId);

    /**
     * Carga en una sola query las asignaciones de horario de múltiples usuarios.
     * Usado para eliminar el problema N+1 al construir páginas de usuarios.
     *
     * @param usuarioIds Lista de IDs de usuarios a consultar.
     * @return Lista de asignaciones con el horario ya cargado (JOIN FETCH).
     */
    @Query("SELECT hu FROM HorarioUsuario hu JOIN FETCH hu.horario WHERE hu.usuario.id IN :usuarioIds")
    List<HorarioUsuario> findByUsuarioIdIn(@Param("usuarioIds") List<Integer> usuarioIds);
}