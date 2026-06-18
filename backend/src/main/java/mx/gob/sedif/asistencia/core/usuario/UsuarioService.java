package mx.gob.sedif.asistencia.core.usuario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.sedif.asistencia.core.area.Area;
import mx.gob.sedif.asistencia.core.area.AreaRepository;
import mx.gob.sedif.asistencia.core.area.AreaService;
import mx.gob.sedif.asistencia.security.SecurityUtil;
import mx.gob.sedif.asistencia.util.ExportExcelService;
import mx.gob.sedif.asistencia.util.enums.Estado;
import mx.gob.sedif.asistencia.util.enums.Rol;
import mx.gob.sedif.asistencia.core.horario.Horario;
import mx.gob.sedif.asistencia.core.horario.HorarioRepository;
import mx.gob.sedif.asistencia.core.horario.HorarioUsuario;
import mx.gob.sedif.asistencia.core.horario.HorarioUsuarioId;
import mx.gob.sedif.asistencia.core.horario.HorarioUsuarioRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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
    private final ExportExcelService exportExcelService;

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
    public Page<UsuarioRecord> getAll(String key, String numeroControl, Integer areaId, Pageable pageable) {
        Usuario currentUser = securityUtil.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        Page<Usuario> usuariosPage;
        
        // Convertir strings vacíos a null para que funcione el IS NULL de la consulta
        String searchNombre = (key == null || key.isBlank()) ? null : key;
        String searchNumCtrl = (numeroControl == null || numeroControl.isBlank()) ? null : numeroControl;

        if (currentUser.getRol() == Rol.SUPERADMIN) {
            usuariosPage = usuarioRepository.findByFiltros(searchNumCtrl, searchNombre, areaId, pageable);
        } else if (currentUser.getRol() == Rol.ADMIN) {
            Set<Integer> idsPermitidos = areaService.obtenerIdsDeAreasGestionadasPorAdmin(currentUser);
            if (idsPermitidos.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            usuariosPage = usuarioRepository.findByFiltrosAndAreasPermitidas(searchNumCtrl, searchNombre, areaId, idsPermitidos, pageable);
        } else {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Batch load de horarios: una sola query para toda la página en lugar de
        // una por usuario (PERF-003). El mapa permite mapear cada usuario sin N+1.
        List<Integer> idsPagina = usuariosPage.getContent().stream()
                .map(Usuario::getId)
                .toList();

        Map<Integer, HorarioUsuario> horariosPorUsuario = idsPagina.isEmpty()
                ? Collections.emptyMap()
                : horarioUsuarioRepository.findByUsuarioIdIn(idsPagina).stream()
                    .collect(Collectors.toMap(
                            hu -> hu.getUsuario().getId(),
                            hu -> hu,
                            (a, b) -> a));

        return usuariosPage.map(usuario -> this.toRecord(usuario, horariosPorUsuario.get(usuario.getId())));
    }

    /**
     * Genera el Excel de usuarios activos según los filtros recibidos. Reutiliza
     * {@link #getAll} (sin paginar) para respetar los permisos por rol y el
     * filtrado por número de control / área. Solo incluye usuarios ACTIVOS.
     *
     * <p>Columnas: No. Control, Nombre completo, Área, Horario.
     *
     * @param key           búsqueda por nombre (opcional)
     * @param numeroControl número de control exacto/parcial (opcional)
     * @param areaId        área a filtrar (opcional)
     */
    @Transactional(readOnly = true)
    public byte[] exportarExcel(String key, String numeroControl, Integer areaId) throws IOException {
        Pageable sinPaginar = Pageable.unpaged(Sort.by(Sort.Direction.ASC, "numeroControl"));

        List<UsuarioRecord> usuarios = getAll(key, numeroControl, areaId, sinPaginar).getContent().stream()
                .filter(u -> u.estatus() == Estado.ACTIVE)
                .collect(Collectors.toList());

        String[] headers = { "No. Control", "Nombre completo", "Área", "Horario" };
        List<Function<UsuarioRecord, String>> extractores = List.of(
                UsuarioRecord::numeroControl,
                UsuarioRecord::nombreCompleto,
                UsuarioRecord::nombreAreaPrincipal,
                u -> u.nombreHorarioAsignado() != null ? u.nombreHorarioAsignado() : "Sin horario asignado"
        );

        return exportExcelService.generar("Usuarios", headers, usuarios, extractores);
    }

    /**
     * Procesa una carga masiva de usuarios desde un archivo Excel.
     *
     * <p>Columnas esperadas (fila 1 = encabezados, los datos inician en la fila 2):
     * <ol start="0">
     *   <li>numero_control</li>
     *   <li>nombre</li>
     *   <li>apellido_paterno</li>
     *   <li>apellido_materno (opcional)</li>
     *   <li>area_id</li>
     *   <li>rol (debe ser USER)</li>
     * </ol>
     *
     * <p>Reglas: todos quedan con estatus ACTIVE; la contraseña se genera
     * automáticamente (numeroControl + "-DIF") y se exige cambio al primer
     * ingreso. Solo se permite el rol USER en la carga masiva. Cada fila se
     * valida de forma independiente: si una falla, se reporta con su número de
     * fila y el resto continúa. Si un ADMIN realiza la carga, solo puede dar de
     * alta usuarios en las áreas que gestiona.
     *
     * @return mapa con: procesados (int), errores (int), detalleErrores (List&lt;String&gt;)
     */
    @CacheEvict(value = "areasAdmin", allEntries = true)
    @Transactional
    public Map<String, Object> procesarCargaMasivaUsuarios(MultipartFile file) throws IOException {
        Usuario currentUser = securityUtil.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        // Si es ADMIN, solo puede cargar usuarios en sus áreas gestionadas.
        Set<Integer> idsAreasPermitidas = currentUser.getRol() == Rol.ADMIN
                ? areaService.obtenerIdsDeAreasGestionadasPorAdmin(currentUser)
                : null; // null => SUPERADMIN, sin restricción

        int procesados = 0;
        int errores = 0;
        List<String> detalleErrores = new ArrayList<>();

        // Detecta números de control repetidos dentro del propio archivo.
        Set<String> numerosControlEnArchivo = new HashSet<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // saltar encabezados

                int numeroFila = row.getRowNum() + 1; // fila tal como la ve el usuario en Excel

                // Saltar filas totalmente vacías sin contarlas como error.
                if (filaVacia(row)) continue;

                try {
                    String numeroControl = getCellValueAsString(row.getCell(0));
                    String nombre = getCellValueAsString(row.getCell(1));
                    String apellidoPaterno = getCellValueAsString(row.getCell(2));
                    String apellidoMaterno = getCellValueAsString(row.getCell(3));
                    String areaIdTexto = getCellValueAsString(row.getCell(4));
                    String rolTexto = getCellValueAsString(row.getCell(5)).toUpperCase();

                    // --- Validaciones de campos obligatorios ---
                    if (numeroControl.isBlank()) {
                        throw new IllegalArgumentException("El número de control es obligatorio.");
                    }
                    if (nombre.isBlank()) {
                        throw new IllegalArgumentException("El nombre es obligatorio.");
                    }
                    if (apellidoPaterno.isBlank()) {
                        throw new IllegalArgumentException("El apellido paterno es obligatorio.");
                    }

                    // --- Rol: solo USER en carga masiva ---
                    if (rolTexto.isBlank()) {
                        throw new IllegalArgumentException("El rol es obligatorio (debe ser USER).");
                    }
                    Rol rol;
                    try {
                        rol = Rol.valueOf(rolTexto);
                    } catch (IllegalArgumentException ex) {
                        throw new IllegalArgumentException("Rol inválido '" + rolTexto + "'. Solo se permite USER.");
                    }
                    if (rol != Rol.USER) {
                        throw new IllegalArgumentException("Solo se permite el rol USER en la carga masiva. "
                                + "Los administradores se crean de forma individual.");
                    }

                    // --- Área ---
                    if (areaIdTexto.isBlank()) {
                        throw new IllegalArgumentException("El area_id es obligatorio.");
                    }
                    Integer areaId;
                    try {
                        areaId = Integer.valueOf(areaIdTexto);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("El area_id '" + areaIdTexto + "' no es un número válido.");
                    }
                    Area area = areaRepository.findById(areaId)
                            .orElseThrow(() -> new IllegalArgumentException("No existe un área con id " + areaId + "."));
                    if (area.getEstatus() != Estado.ACTIVE) {
                        throw new IllegalArgumentException("El área con id " + areaId + " no está activa.");
                    }
                    if (idsAreasPermitidas != null && !idsAreasPermitidas.contains(areaId)) {
                        throw new SecurityException("No tiene permisos para dar de alta usuarios en el área con id " + areaId + ".");
                    }

                    // --- Duplicados: dentro del archivo y contra la base de datos ---
                    if (!numerosControlEnArchivo.add(numeroControl)) {
                        throw new IllegalArgumentException("El número de control '" + numeroControl
                                + "' está repetido dentro del archivo.");
                    }
                    if (usuarioRepository.findByNumeroControl(numeroControl).isPresent()) {
                        throw new IllegalArgumentException("El número de control '" + numeroControl
                                + "' ya existe en el sistema.");
                    }

                    // --- Alta del usuario ---
                    Usuario usuario = new Usuario();
                    usuario.setNumeroControl(numeroControl);
                    usuario.setNombre(nombre);
                    usuario.setApellidoPaterno(apellidoPaterno);
                    usuario.setApellidoMaterno(apellidoMaterno.isBlank() ? null : apellidoMaterno);
                    usuario.setRol(Rol.USER);
                    usuario.setEstatus(Estado.ACTIVE);
                    usuario.setAreaPrincipal(area);
                    usuario.setAreasGestionadas(new HashSet<>());
                    // Contraseña inicial predeterminada; se obliga a cambiarla en el primer login.
                    usuario.setPassword(passwordEncoder.encode(numeroControl + "-DIF"));
                    usuario.setRequiereCambioPassword(true);

                    usuarioRepository.save(usuario);
                    procesados++;

                } catch (Exception e) {
                    errores++;
                    detalleErrores.add("Fila " + numeroFila + ": " + e.getMessage());
                }
            }
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("procesados", procesados);
        resultado.put("errores", errores);
        resultado.put("detalleErrores", detalleErrores);
        return resultado;
    }

    /** True si todas las celdas relevantes de la fila están vacías. */
    private boolean filaVacia(Row row) {
        for (int i = 0; i <= 5; i++) {
            if (!getCellValueAsString(row.getCell(i)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    /** Lee una celda como texto, tolerando celdas numéricas y nulas. */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
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
    @CacheEvict(value = "areasAdmin", allEntries = true)
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
    @CacheEvict(value = "areasAdmin", allEntries = true)
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
    public void cambiarMiContrasena(String numeroControl, String contrasenaActual, String nuevaContrasena) {
        Usuario usuario = usuarioRepository.findByNumeroControl(numeroControl)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Si NO es el cambio obligatorio de primer acceso, hay que verificar la
        // contraseña actual para evitar el secuestro de cuentas con un token robado.
        if (Boolean.FALSE.equals(usuario.getRequiereCambioPassword())) {
            if (contrasenaActual == null
                    || !passwordEncoder.matches(contrasenaActual, usuario.getPassword())) {
                throw new SecurityException("La contraseña actual es incorrecta.");
            }
        }

        usuario.setPassword(passwordEncoder.encode(nuevaContrasena));
        usuario.setRequiereCambioPassword(false); // Apagamos la alerta
        usuarioRepository.save(usuario);
    }
}