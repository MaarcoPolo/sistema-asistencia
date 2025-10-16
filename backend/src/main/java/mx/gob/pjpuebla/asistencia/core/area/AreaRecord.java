package mx.gob.pjpuebla.asistencia.core.area;

import java.io.Serializable;

import mx.gob.pjpuebla.asistencia.util.enums.Estado;

public record AreaRecord(
    Integer id,
    String clave,
    String nombre,
    Estado estatus,
    String estatusEtiqueta,
    Integer idAreaPadre,
    String nombreAreaPadre,
    String ipPermitida
) implements Serializable {}