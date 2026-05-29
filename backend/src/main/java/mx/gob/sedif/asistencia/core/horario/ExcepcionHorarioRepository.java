package mx.gob.sedif.asistencia.core.horario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExcepcionHorarioRepository extends JpaRepository<ExcepcionHorario, Integer> {
    
    // Busca si hay una regla manual dictada por RH para un usuario en un día exacto
    Optional<ExcepcionHorario> findByUsuarioIdAndFechaEspecifica(Integer usuarioId, LocalDate fecha);
}