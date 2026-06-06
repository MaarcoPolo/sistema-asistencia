package mx.gob.sedif.asistencia.core.usuario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.sedif.asistencia.core.area.Area;
import mx.gob.sedif.asistencia.core.area.AreaRepository;
import mx.gob.sedif.asistencia.core.area.AreaService;
import mx.gob.sedif.asistencia.security.SecurityUtil;
import mx.gob.sedif.asistencia.util.enums.Estado;
import mx.gob.sedif.asistencia.util.enums.Rol;
import mx.gob.sedif.asistencia.core.horario.Horario;
import mx.gob.sedif.asistencia.core.horario.HorarioRepository;
import mx.gob.sedif.asistencia.core.horario.HorarioUsuario;
import mx.gob.sedif.asistencia.core.horario.HorarioUsuarioId;
import mx.gob.sedif.asistencia.core.horario.HorarioUsuarioRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de negocio para la gestión de usuarios del sistema.
 *
 * <p>Aplica reglas de autorización por rol antes de cualquier operación:
 * SUPERADMIN puede ver y modificar todo; ADMIN solo opera sobre las áreas
 * que gestiona y no puede crear/editar usuarios con privilegios superiores.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;
    private final AreaService areaService;
    private final HorarioRepository horarioRepository;
    private final HorarioUsuarioRepository horarioUsuarioRepository;

    /**
     * Retorna una página de usuarios filtrados por nombre, aplicando
     * las restricciones de visibilidad según el rol del usuario autenticado.
     *
     * <p>Para evitar el problema N+1, los horarios asignados se cargan en
     * una sola query adicional para toda la página (batch load).
     *
     * @param key      Texto de búsqueda sobre nombre completo (puede ser vacío).
     * @param pageable Configuración de paginación y ordenamiento.
     * @return Página de {@link UsuarioRecord} con datos de presentación incluidos.
     */
    @Transactional(readOnly = true)
    public Page<UsuarioRecord> getAll(String key, Pageable pageable) {
        Usuario currentUser = securityUtil.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        Page<Usuario> usuariosPage;

        if (currentUser.getRol() == Rol.SUPERADMIN) {
            usuariosPage = usuarioRepository.findByNombreCompletoContaining(key, pageable);
        } else if (currentUser.getRol() == Rol.ADMIN) {
            Set<Integer> idsDeSusAreas = areaService.obtenerIdsDeAreasGestionadasPorAdmin(currentUser);
            if (idsDeSusAreas.isEmpty()) {
                return Page.empty(pageable);
            }
            usuariosPage = usuarioRepository.findByNombreCompletoInAreaIds(key, idsDeSusAreas, pageable);
        } else {
            return Page.empty(pageable);
        }

        // Batch load de horarios: una sola query para todos los usuarios de la página
        List<Integer> idsEnPagina = usuariosPage.getContent().stream()
                .map(Usuario::getId)
                .collect(Collectors.toList());

        Map<Integer, HorarioUsuario> horariosPorUsuario = horarioUsuarioRepository
                .findByUsuarioIdIn(idsEnPagina)
                .stream()
                .collect(Collectors.toMap(hu -> hu.getUsuario().getId(), hu -> hu));

        List<UsuarioRecord> records = usuariosPage.getContent().stream()
                .map(u -> toRecord(u, horariosPorUsuario.get(u.getId())))
                .collect(Collectors.toList());

        return new PageImpl<>(records, pageable, usuariosPage.getTotalElements());
    }

    /**
     * Busca un usuario por ID verificando que el usuario autenticado tenga
     * permisos para verlo (ADMIN solo ve usuarios de sus áreas).
     */
    @Transactional(readOnly = true)
    public UsuarioRecord findById(Integer id) {
        Usuario currentUser = securityUtil.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
        Usuario usuarioEncontrado = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (currentUser.getRol() == Rol.ADMIN) {
            Set<Integer> idsPermitidos = areaService.obtenerIdsDeAreasGestionadasPorAdmin(currentUser);
            if (!idsPermitidos.contains(usuarioEncontrado.getAreaPrincipal().getId())) {
                throw new SecurityException("No tiene permisos para ver este usuario.");
            }
        }
        return toRecord(usuarioEncontrado);
    }

    /**
     * Crea un nuevo usuario aplicando las restricciones de rol:
     * <ul>
     *   <li>ADMIN solo puede crear usuarios en sus áreas gestionadas y con rol USER.</li>
     *   <li>Usuarios de tipo USER reciben contraseña inicial predeterminada.</li>
     *   <li>Usuarios de tipo ADMIN/SUPERADMIN deben especificar contraseña explícita.</li>
     * </ul>
     */
    @Transactional
    public UsuarioRecord create(UsuarioRecord record) {
        Usuario currentUser = securityUtil.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        if (currentUser.getRol() == Rol.ADMIN) {
            Set<Integer> idsPermitidos = areaService.obtenerIdsDeAreasGestionadasPorAdmin(currentUser);
            if (!idsPermitidos.contains(record.idAreaPrincipal())) {
                throw new SecurityException("No puede crear usuarios en un área que no gestiona.");
            }
            if (record.rol() == Rol.SUPERADMIN || record.rol() == Rol.ADMIN) {
                throw new SecurityException("No tiene permisos para crear usuarios con este rol.");
            }
        }

        if (usuarioRepository.findByNumeroControl(record.numeroControl()).isPresent()) {
            throw new RuntimeException("El número de control ya está en uso.");
        }

        Usuario entity = new Usuario();
        this.mapToEntity(record, entity);

        if (record.rol() == Rol.ADMIN || record.rol() == Rol.SUPERADMIN) {
            if (record.password() == null || record.password().isBlank()) {
                throw new RuntimeException("La contraseña es obligatoria para crear un usuario Administrador.");
            }
            entity.setPassword(passwordEncoder.encode(record.password()));
            entity.setRequiereCambioPassword(false);
        } else {
            // Contraseña inicial predeterminada para usuarios básicos; se les pide cambiarla al primer login.
            entity.setPassword(passwordEncoder.encode(record.numeroControl() + "-DIF"));
            entity.setRequiereCambioPassword(true);
        }

        entity = usuarioRepository.save(entity);
        this.asignarHorario(entity, record.idHorarioAsignado());
        return toRecord(entity);
    }

    /**
     * Actualiza los datos de un usuario existente con las mismas restricciones de
     * visibilidad y rol que en {@link #create}.
     * Si {@code password} viene vacío/nulo, la contraseña existente no se modifica.
     */
    @Transactional
    public UsuarioRecord save(UsuarioRecord record) {
        Usuario currentUser = securityUtil.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
        Usuario entityToUpdate = usuarioRepository.findById(record.id())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (currentUser.getRol() == Rol.ADMIN) {
            if (entityToUpdate.getRol() == Rol.SUPERADMIN) {
                throw new SecurityException("No tiene permisos para editar a un Superadministrador.");
            }
            Set<Integer> idsPermitidos = areaService.obtenerIdsDeAreasGestionadasPorAdmin(currentUser);
            if (!idsPermitidos.contains(entityToUpdate.getAreaPrincipal().getId())
                    || !idsPermitidos.contains(record.idAreaPrincipal())) {
                throw new SecurityException("No tiene permisos para editar usuarios de esta área o moverlos a un área no gestionada.");
            }
        }

        Optional<Usuario> usuarioExistente = usuarioRepository.findByNumeroControl(record.numeroControl());
        if (usuarioExistente.isPresent() && !usuarioExistente.get().getId().equals(entityToUpdate.getId())) {
            throw new RuntimeException("El número de control ya está en uso por otro usuario.");
        }

        this.mapToEntity(record, entityToUpdate);

        if (record.password() != null && !record.password().isBlank()) {
            entityToUpdate.setPassword(passwordEncoder.encode(record.password()));
        }

        entityToUpdate = usuarioRepository.save(entityToUpdate);
        this.asignarHorario(entityToUpdate, record.idHorarioAsignado());
        return toRecord(entityToUpdate);
    }

    /**
     * Aplica un soft-delete (estatus = DELETED) al usuario con el ID dado.
     * ADMIN solo puede eliminar usuarios de sus áreas.
     */
    @Transactional
    public void deleteById(Integer id) {
        Usuario currentUser = securityUtil.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
        Usuario entity = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado para eliminar"));

        if (currentUser.getRol() == Rol.ADMIN) {
            Set<Integer> idsPermitidos = areaService.obtenerIdsDeAreasGestionadasPorAdmin(currentUser);
            if (!idsPermitidos.contains(entity.getAreaPrincipal().getId())) {
                throw new SecurityException("No tiene permisos para eliminar este usuario.");
            }
        }

        entity.setEstatus(Estado.DELETED);
        usuarioRepository.save(entity);
    }

    /**
     * Asigna o actualiza el horario de trabajo de un usuario en la tabla intermedia.
     * Si el usuario ya tenía un horario, lo reemplaza manteniendo la fecha de inicio
     * de ciclo original para no romper el cálculo de turnos rotativos.
     *
     * @param usuario   Usuario al que se le asigna el horario.
     * @param idHorario ID del horario a asignar; si es null, no hace nada.
     */
    private void asignarHorario(Usuario usuario, Integer idHorario) {
        if (idHorario == null) return;

        Horario horario = horarioRepository.findById(idHorario)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado"));

        HorarioUsuario horarioUsuario = horarioUsuarioRepository.findByUsuarioId(usuario.getId())
                .orElse(new HorarioUsuario());

        HorarioUsuarioId id = new HorarioUsuarioId();
        id.setUsuarioId(usuario.getId());
        id.setHorarioId(horario.getId());

        horarioUsuario.setId(id);
        horarioUsuario.setUsuario(usuario);
        horarioUsuario.setHorario(horario);
        horarioUsuario.setArea(usuario.getAreaPrincipal());

        if (horarioUsuario.getFechaInicioCiclo() == null) {
            horarioUsuario.setFechaInicioCiclo(LocalDate.now());
        }

        horarioUsuarioRepository.save(horarioUsuario);
    }

    // ── Métodos de Mapeo ──────────────────────────────────────────────────────

    /**
     * Convierte una entidad {@link Usuario} a su DTO de respuesta.
     * Usa el {@link HorarioUsuario} ya cargado externamente para evitar N+1.
     *
     * @param entity         Entidad usuario con areaPrincipal cargada (JOIN FETCH).
     * @param horarioUsuario Asignación de horario pre-cargada en batch; puede ser null.
     * @return DTO con todos los campos de presentación calculados.
     */
    private UsuarioRecord toRecord(Usuario entity, HorarioUsuario horarioUsuario) {
        String nombreCompleto = entity.getNombre() + " " + entity.getApellidoPaterno() +
                (entity.getApellidoMaterno() != null ? " " + entity.getApellidoMaterno() : "");

        Set<Integer> idsAreasGestionadas = entity.getAreasGestionadas() != null
                ? entity.getAreasGestionadas().stream().map(Area::getId).collect(Collectors.toSet())
                : new HashSet<>();

        Integer idHorario = horarioUsuario != null ? horarioUsuario.getHorario().getId() : null;
        String nombreHorario = horarioUsuario != null ? horarioUsuario.getHorario().getNombre() : null;

        return new UsuarioRecord(
                entity.getId(),
                entity.getNumeroControl(),
                entity.getNombre(),
                entity.getApellidoPaterno(),
                entity.getApellidoMaterno(),
                null,   // password nunca se serializa en respuestas
                entity.getRol(),
                entity.getEstatus(),
                entity.getAreaPrincipal().getId(),
                idsAreasGestionadas,
                nombreCompleto.trim(),
                entity.getRol().getEtiqueta(),
                entity.getEstatus().getEtiqueta(),
                entity.getAreaPrincipal().getNombre(),
                idHorario,
                nombreHorario,
                entity.getRequiereCambioPassword()
        );
    }

    /**
     * Sobrecarga de conveniencia para los métodos que trabajan con un único usuario
     * (findById, create, save) y hacen su propia consulta de horario.
     */
    private UsuarioRecord toRecord(Usuario entity) {
        Optional<HorarioUsuario> hu = horarioUsuarioRepository.findByUsuarioId(entity.getId());
        return toRecord(entity, hu.orElse(null));
    }

    private void mapToEntity(UsuarioRecord record, Usuario entity) {
        Area areaPrincipal = areaRepository.findById(record.idAreaPrincipal()).orElseThrow(() -> new RuntimeException("Área principal no encontrada"));
        
        entity.setNumeroControl(record.numeroControl());
        entity.setNombre(record.nombre());
        entity.setApellidoPaterno(record.apellidoPaterno());
        entity.setApellidoMaterno(record.apellidoMaterno());
        entity.setRol(record.rol());
        entity.setEstatus(record.estatus());
        entity.setAreaPrincipal(areaPrincipal);

        if (record.rol() == Rol.ADMIN && record.idsAreasGestionadas() != null) {
            Set<Area> areasGestionadas = new HashSet<>(areaRepository.findAllById(record.idsAreasGestionadas()));
            entity.setAreasGestionadas(areasGestionadas);
        } else {
            entity.setAreasGestionadas(new HashSet<>()); 
        }
    }

    @Transactional(readOnly = true)
    public UsuarioRecord findByNumeroControl(String numeroControl) {
        Usuario entity = usuarioRepository.findByNumeroControl(numeroControl)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return toRecord(entity);
    }

    @Transactional
    public void resetPassword(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        String passwordDefault = usuario.getNumeroControl() + "-DIF";
        usuario.setPassword(passwordEncoder.encode(passwordDefault));
        usuario.setRequiereCambioPassword(true); // Se vuelve a prender la alerta
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void cambiarMiContrasena(String numeroControl, String nuevaContrasena) {
        Usuario usuario = usuarioRepository.findByNumeroControl(numeroControl)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        usuario.setPassword(passwordEncoder.encode(nuevaContrasena));
        usuario.setRequiereCambioPassword(false); // Apagamos la alerta
        usuarioRepository.save(usuario);
    }
}