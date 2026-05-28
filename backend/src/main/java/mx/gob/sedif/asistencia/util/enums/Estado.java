package mx.gob.sedif.asistencia.util.enums;

import lombok.Getter;

@Getter
public enum Estado {
    ACTIVE("Activo"),
    INACTIVE("Inactivo"),
    DELETED("Eliminado");

    private final String etiqueta;

    Estado(String etiqueta){
        this.etiqueta = etiqueta;
    }
}
