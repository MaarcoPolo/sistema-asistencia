package mx.gob.sedif.asistencia.core.horario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mx.gob.sedif.asistencia.core.usuario.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "tbl_excepcion_horario", schema = "asistencia")
public class ExcepcionHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "d_fecha_especifica", nullable = false)
    private LocalDate fechaEspecifica;

    @Column(name = "b_labora", nullable = false)
    private Boolean labora;

    @Column(name = "s_motivo")
    private String motivo;

    @Column(name = "t_fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "s_usuario_alta", nullable = false, updatable = false)
    private String usuarioAlta;

    @PrePersist
    public void prePersist() {
        this.fechaAlta = LocalDateTime.now();
    }
}