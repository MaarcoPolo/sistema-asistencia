package mx.gob.pjpuebla.asistencia.util.enums;

import lombok.Getter;

@Getter
public enum Rol {
    SUPERADMIN("Superadministrador"),
    ADMIN("Administrador de Área"),
    USER("Usuario");

    private final String etiqueta;

    private Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}