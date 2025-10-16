package mx.gob.pjpuebla.asistencia.core.asistencia;

import mx.gob.pjpuebla.asistencia.core.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long>, JpaSpecificationExecutor<Asistencia> {

    // verificar si ya existe un registro para un usuario en un día específico
    Optional<Asistencia> findByUsuarioAndFecha(Usuario usuario, LocalDate fecha);
}