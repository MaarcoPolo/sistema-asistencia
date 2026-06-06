package mx.gob.sedif.asistencia.core.asistencia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mx.gob.sedif.asistencia.core.justificacion.AsistenciaJustificacion;
import mx.gob.sedif.asistencia.core.usuario.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "tbl_asistencia", schema = "asistencia")
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "d_fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "t_hora_entrada")
    private LocalDateTime horaEntrada;
    
    @Column(name = "t_hora_salida")
    private LocalDateTime horaSalida;

    @Lob
    @Column(name = "s_foto_entrada", columnDefinition = "TEXT")
    private String fotoEntrada;

    @Lob
    @Column(name = "s_foto_salida", columnDefinition = "TEXT")
    private String fotoSalida;
    
    @Column(name = "s_ip_registro")
    private String ipRegistro;

    // 0=OK, 1=Retardo, 2=Falta Total, 3=Omisión Entrada, 4=Omisión Salida
    @Column(name = "n_estatus_incidencia", nullable = false)
    private Integer estatusIncidencia = 0;

    @OneToOne(mappedBy = "asistencia", fetch = FetchType.LAZY)
    private AsistenciaJustificacion justificacionAplicada;
}