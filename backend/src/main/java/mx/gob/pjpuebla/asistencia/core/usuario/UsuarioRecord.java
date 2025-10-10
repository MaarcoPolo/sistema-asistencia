package mx.gob.pjpuebla.asistencia.core.usuario;

import java.io.Serializable;

import mx.gob.pjpuebla.asistencia.util.enums.Estado;
import mx.gob.pjpuebla.asistencia.util.enums.Rol;
import java.util.Set;


// Este record servirá tanto para la creación/edición como para la visualización
public record UsuarioRecord(
    Integer id,
    String matricula,
    String nombre,
    String apellidoPaterno,
    String apellidoMaterno,
    String password, // Se usará solo para crear/actualizar, nunca se devolverá poblado
    Rol rol,
    Estado estatus,
    Integer idAreaPrincipal, 
    Set<Integer> idsAreasGestionadas, 
    
    String nombreCompleto,
    String rolEtiqueta,
    String estatusEtiqueta,
    String nombreAreaPrincipal
) implements Serializable {}