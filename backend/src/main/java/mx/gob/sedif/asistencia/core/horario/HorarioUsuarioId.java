package mx.gob.sedif.asistencia.core.horario;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
public class HorarioUsuarioId implements Serializable {
    
    @Column(name = "fn_usuario_id")
    private Integer usuarioId;

    @Column(name = "fn_horario_id")
    private Integer horarioId;
}