package mx.gob.pjpuebla.asistencia.core.area;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mx.gob.pjpuebla.asistencia.util.Audit;
import mx.gob.pjpuebla.asistencia.util.Auditable;
import mx.gob.pjpuebla.asistencia.util.AuditableListener;
import mx.gob.pjpuebla.asistencia.util.enums.Estado;

import java.io.Serializable;

@Entity
@Getter
@Setter
@EntityListeners(AuditableListener.class)
@Table(name = "tbl_area", schema = "asistencia")
public class Area implements Serializable, Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Integer id;

    @Column(name = "s_clave", unique = true, nullable = false)
    private String clave;

    @Column(name = "s_nombre", nullable = false)
    private String nombre;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "n_estatus", nullable = false)
    private Estado estatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_area_padre")
    @JsonIgnore
    private Area areaPadre;

    @Column(name = "s_ip_permitida")
    private String ipPermitida;

    @Embedded
    private Audit audit;
}