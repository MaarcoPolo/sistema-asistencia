package mx.gob.sedif.asistencia.core.justificacion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "tbl_catalogo_justificacion", schema = "asistencia")
public class CatalogoJustificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Integer id;

    @Column(name = "s_clave", nullable = false, unique = true)
    private String clave;

    @Column(name = "s_nombre", nullable = false)
    private String nombre;

    @Column(name = "b_requiere_observacion", nullable = false)
    private Boolean requiereObservacion = false;
}