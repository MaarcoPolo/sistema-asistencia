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

    Optional<Usuario> findByMatricula(String matricula);

    @Query("SELECT u FROM Usuario u WHERE lower(concat(u.nombre, ' ', u.apellidoPaterno, ' ', u.apellidoMaterno)) LIKE lower(concat('%', ?1, '%'))")
    Page<Usuario> findByNombreCompletoContaining(String nombre, Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE u.areaPrincipal.id IN :areaIds AND lower(concat(u.nombre, ' ', u.apellidoPaterno, ' ', u.apellidoMaterno)) LIKE lower(concat('%', :nombre, '%'))")
    Page<Usuario> findByNombreCompletoInAreaIds(String nombre, Collection<Integer> areaIds, Pageable pageable);
}