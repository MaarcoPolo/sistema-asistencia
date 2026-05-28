package mx.gob.sedif.asistencia.core.horario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mx.gob.sedif.asistencia.core.area.Area;
import mx.gob.sedif.asistencia.core.usuario.Usuario;
import mx.gob.sedif.asistencia.util.Audit;
import mx.gob.sedif.asistencia.util.Auditable;
import mx.gob.sedif.asistencia.util.AuditableListener;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@EntityListeners(AuditableListener.class)
@Table(name = "tbl_horario_usuario", schema = "asistencia")
public class HorarioUsuario implements Auditable {

    @EmbeddedId
    private HorarioUsuarioId id = new HorarioUsuarioId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "fn_usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("horarioId")
    @JoinColumn(name = "fn_horario_id")
    private Horario horario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_area_id", nullable = false)
    private Area area;

    @Column(name = "d_fecha_inicio_ciclo")
    private LocalDate fechaInicioCiclo;

    @Embedded
    private Audit audit;
}