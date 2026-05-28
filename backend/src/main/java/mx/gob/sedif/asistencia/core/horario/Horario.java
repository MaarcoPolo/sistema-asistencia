package mx.gob.sedif.asistencia.core.horario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mx.gob.sedif.asistencia.util.Audit;
import mx.gob.sedif.asistencia.util.Auditable;
import mx.gob.sedif.asistencia.util.AuditableListener;

import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "b_cruce_medianoche", nullable = false)
    private Boolean cruceMedianoche = false;

    @Column(name = "n_tipo_ciclo", nullable = false)
    private Integer tipoCiclo = 1;

    @OneToMany(mappedBy = "horario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HorarioDetalle> detalles = new ArrayList<>();

    @Embedded
    private Audit audit;
}