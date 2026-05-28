package mx.gob.sedif.asistencia.core.asistencia;

import mx.gob.sedif.asistencia.core.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long>, JpaSpecificationExecutor<Asistencia> {
    Optional<Asistencia> findByUsuarioAndFecha(Usuario usuario, LocalDate fecha);
}