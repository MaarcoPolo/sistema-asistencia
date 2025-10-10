package mx.gob.pjpuebla.asistencia.core.horario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mx.gob.pjpuebla.asistencia.core.usuario.Usuario;
import mx.gob.pjpuebla.asistencia.util.Audit;
import mx.gob.pjpuebla.asistencia.util.Auditable;
import mx.gob.pjpuebla.asistencia.util.AuditableListener;
import mx.gob.pjpuebla.asistencia.core.area.Area;


import java.time.LocalTime;

@Entity
@Getter
@Setter
@EntityListeners(AuditableListener.class)
@Table(name = "tbl_horario", schema = "asistencia")
public class Horario implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Integer id;

    @Column(name = "s_nombre", nullable = false)
    private String nombre;

    @Column(name = "t_hora_entrada", nullable = false)
    private LocalTime horaEntrada;

    @Column(name = "t_hora_salida", nullable = false)
    private LocalTime horaSalida;

    @Column(name = "n_tolerancia_minutos", nullable = false)
    private Integer toleranciaMinutos;

    @OneToOne
    @JoinColumn(name = "fn_usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_area_id")
    private Area area;

    @Embedded
    private Audit audit;
}