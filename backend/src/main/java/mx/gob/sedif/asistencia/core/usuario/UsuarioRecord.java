package mx.gob.sedif.asistencia.core.usuario;

import java.io.Serializable;
import mx.gob.sedif.asistencia.util.enums.Estado;
import mx.gob.sedif.asistencia.util.enums.Rol;
import java.util.Set;

public record UsuarioRecord(
    Integer id,
    String numeroControl, 
    String nombre,
    String apellidoPaterno,
    String apellidoMaterno,
    String password, 
    Rol rol,
    Estado estatus,
    Integer idAreaPrincipal, 
    Set<Integer> idsAreasGestionadas, 
    
    String nombreCompleto,
    String rolEtiqueta,
    String estatusEtiqueta,
    String nombreAreaPrincipal,
    
    Integer idHorarioAsignado,
    String nombreHorarioAsignado,

    Boolean requiereCambioPassword
) implements Serializable {}