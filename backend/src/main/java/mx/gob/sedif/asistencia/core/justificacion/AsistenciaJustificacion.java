package mx.gob.sedif.asistencia.core.justificacion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mx.gob.sedif.asistencia.core.asistencia.Asistencia;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "tbl_asistencia_justificacion", schema = "asistencia")
public class AsistenciaJustificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_asistencia_id", nullable = false, unique = true)
    private Asistencia asistencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_justificacion_id", nullable = false)
    private CatalogoJustificacion justificacion;

    @Column(name = "s_observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "s_ruta_pdf", length = 500)
    private String rutaPdf;

    @Column(name = "t_fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "s_usuario_registro", nullable = false, updatable = false)
    private String usuarioRegistro;

    @PrePersist
    public void prePersist() {
        this.fechaRegistro = LocalDateTime.now();
    }
}