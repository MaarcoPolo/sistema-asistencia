package mx.gob.sedif.asistencia.core.horario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HorarioUsuarioRepository extends JpaRepository<HorarioUsuario, HorarioUsuarioId> {
    
    // Busca el horario asignado a un usuario específico
    @Query("SELECT hu FROM HorarioUsuario hu WHERE hu.usuario.id = :usuarioId")
    Optional<HorarioUsuario> findByUsuarioId(@Param("usuarioId") Integer usuarioId);
}