package mx.gob.sedif.asistencia.core.horario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Getter
@Setter
@Table(name = "tbl_horario_detalle", schema = "asistencia")
public class HorarioDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_horario_id", nullable = false)
    private Horario horario;

    @Column(name = "n_dia", nullable = false)
    private Integer dia;

    @Column(name = "t_hora_entrada", nullable = false)
    private LocalTime horaEntrada;

    @Column(name = "t_hora_salida", nullable = false)
    private LocalTime horaSalida;

    @Column(name = "n_tolerancia_minutos", nullable = false)
    private Integer toleranciaMinutos = 15;
}