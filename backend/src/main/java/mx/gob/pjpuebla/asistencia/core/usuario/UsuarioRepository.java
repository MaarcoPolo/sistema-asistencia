package mx.gob.pjpuebla.asistencia.core.usuario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Método para buscar usuarios por matrícula, útil para login y validaciones
    Optional<Usuario> findByMatricula(String matricula);

    // JPQL para buscar por nombre completo concatenado
    @Query("SELECT u FROM Usuario u WHERE lower(concat(u.nombre, ' ', u.apellidoPaterno, ' ', u.apellidoMaterno)) LIKE lower(concat('%', ?1, '%'))")
    Page<Usuario> findByNombreCompletoContaining(String nombre, Pageable pageable);

   // Esta consulta busca por nombre Y donde el área principal del usuario esté DENTRO de una lista de IDs de área.
    @Query("SELECT u FROM Usuario u WHERE u.areaPrincipal.id IN :areaIds AND lower(concat(u.nombre, ' ', u.apellidoPaterno, ' ', u.apellidoMaterno)) LIKE lower(concat('%', :nombre, '%'))")
    Page<Usuario> findByNombreCompletoInAreaIds(String nombre, Collection<Integer> areaIds, Pageable pageable);
}