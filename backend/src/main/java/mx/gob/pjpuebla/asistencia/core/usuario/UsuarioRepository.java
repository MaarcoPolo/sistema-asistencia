package mx.gob.pjpuebla.asistencia.core.usuario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByMatricula(String matricula);

    @Query("SELECT u FROM Usuario u WHERE lower(concat(u.nombre, ' ', u.apellidoPaterno, ' ', u.apellidoMaterno)) LIKE lower(concat('%', ?1, '%'))")
    Page<Usuario> findByNombreCompletoContaining(String nombre, Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE u.areaPrincipal.id IN :areaIds AND lower(concat(u.nombre, ' ', u.apellidoPaterno, ' ', u.apellidoMaterno)) LIKE lower(concat('%', :nombre, '%'))")
    Page<Usuario> findByNombreCompletoInAreaIds(@Param("nombre") String nombre, @Param("areaIds") Set<Integer> areaIds, Pageable pageable);
}