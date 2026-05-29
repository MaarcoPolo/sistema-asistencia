package mx.gob.sedif.asistencia.core.horario;

import lombok.RequiredArgsConstructor;
import mx.gob.sedif.asistencia.core.usuario.Usuario;
import mx.gob.sedif.asistencia.core.usuario.UsuarioRepository;
import mx.gob.sedif.asistencia.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExcepcionHorarioService {

    private final ExcepcionHorarioRepository excepcionHorarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityUtil securityUtil;

   @Transactional
    public ExcepcionHorario crearExcepcion(Integer usuarioId, LocalDate fecha, boolean labora, String motivo) {
        String adminControl = securityUtil.getCurrentUser()
            .map(Usuario::getNumeroControl)
            .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        Usuario empleado = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        // Verificamos si ya existe una regla para ese día y la sobreescribimos
        ExcepcionHorario excepcion = excepcionHorarioRepository
            .findByUsuarioIdAndFechaEspecifica(usuarioId, fecha)
            .orElse(new ExcepcionHorario());

        // CORRECCIÓN: Le pasamos el objeto 'empleado' completo en lugar de su ID
        excepcion.setUsuario(empleado); 
        
        excepcion.setFechaEspecifica(fecha);
        excepcion.setLabora(labora);
        excepcion.setMotivo(motivo);
        excepcion.setFechaAlta(LocalDateTime.now());
        excepcion.setUsuarioAlta(adminControl);

        return excepcionHorarioRepository.save(excepcion);
    }
    
    @Transactional
    public void eliminarExcepcion(Integer id) {
        if(!excepcionHorarioRepository.existsById(id)) {
            throw new RuntimeException("La excepción no existe.");
        }
        excepcionHorarioRepository.deleteById(id);
    }
}