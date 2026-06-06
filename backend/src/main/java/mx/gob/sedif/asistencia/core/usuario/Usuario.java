package mx.gob.sedif.asistencia.core.usuario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mx.gob.sedif.asistencia.core.area.Area;
import mx.gob.sedif.asistencia.util.Audit;
import mx.gob.sedif.asistencia.util.Auditable;
import mx.gob.sedif.asistencia.util.AuditableListener;
import mx.gob.sedif.asistencia.util.enums.Estado;
import mx.gob.sedif.asistencia.util.enums.Rol;

import java.io.Serializable;
import java.util.Set;

@Entity
@Getter
@Setter
@EntityListeners(AuditableListener.class)
@Table(name = "tbl_usuario", schema = "asistencia")
public class Usuario implements Serializable, Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Integer id;

    @Column(name = "s_numero_control", nullable = false, unique = true)
    private String numeroControl;

    @Column(name = "s_nombre", nullable = false)
    private String nombre;

    @Column(name = "s_apellido_paterno", nullable = false)
    private String apellidoPaterno;

    @Column(name = "s_apellido_materno")
    private String apellidoMaterno;
    
    @Column(name = "s_password")
    private String password;

    // EnumType.STRING almacena el nombre del enum ('SUPERADMIN','ADMIN','USER').
    // Es seguro ante reordenamiento del enum; requirió migración V3.
    @Enumerated(EnumType.STRING)
    @Column(name = "n_rol", nullable = false)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(name = "n_estatus", nullable = false)
    private Estado estatus;

    @Column(name = "b_requiere_cambio_password")
    private Boolean requiereCambioPassword = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_area_id", nullable = false)
    private Area areaPrincipal;

    /** Áreas que este usuario (con rol ADMIN) puede administrar. Vacío para rol USER. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "tbl_admin_area", schema = "asistencia",
        joinColumns = @JoinColumn(name = "fn_usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "fn_area_id")
    )
    private Set<Area> areasGestionadas;

    @Embedded
    private Audit audit;
}