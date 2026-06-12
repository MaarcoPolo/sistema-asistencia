package mx.gob.sedif.asistencia.core.asistencia;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.gob.sedif.asistencia.core.area.Area;
import mx.gob.sedif.asistencia.core.area.AreaService;
import mx.gob.sedif.asistencia.core.horario.*;
import mx.gob.sedif.asistencia.core.usuario.Usuario;
import mx.gob.sedif.asistencia.core.usuario.UsuarioRepository;
import mx.gob.sedif.asistencia.security.SecurityUtil;
import mx.gob.sedif.asistencia.util.enums.EstatusJustificacion;
import mx.gob.sedif.asistencia.util.enums.Rol;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import mx.gob.sedif.asistencia.core.justificacion.AsistenciaJustificacion;
import mx.gob.sedif.asistencia.core.justificacion.AsistenciaJustificacionRepository;
import mx.gob.sedif.asistencia.core.justificacion.CatalogoJustificacion;
import mx.gob.sedif.asistencia.core.justificacion.CatalogoJustificacionRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final HorarioUsuarioRepository horarioUsuarioRepository;
    private final ExcepcionHorarioRepository excepcionHorarioRepository;
    private final SecurityUtil securityUtil;
    private final UsuarioRepository usuarioRepository;
    private final AreaService areaService;

    private final AsistenciaJustificacionRepository asistenciaJustificacionRepository;
    private final CatalogoJustificacionRepository catalogoJustificacionRepository;

    // Constantes del Catálogo de Incidencias
    private static final int ESTATUS_OK = 0;
    private static final int ESTATUS_RETARDO = 1;
    private static final int ESTATUS_FALTA_TOTAL = 2;
    private static final int ESTATUS_OMISION_ENTRADA = 3;
    private static final int ESTATUS_OMISION_SALIDA = 4;

    /**
     * Registra la entrada del usuario autenticado para el día actual.
     * Calcula el estatus de incidencia (OK / Retardo / Falta Total) comparando
     * la hora actual con la hora de entrada del horario asignado.
     *
     * <p>Reglas de tiempo (con precisión de segundos):
     * <ul>
     *   <li>Hasta +15:59 min → OK</li>
     *   <li>+16:00 a +30:59 min → Retardo</li>
     *   <li>Más de +30:59 min → Falta Total</li>
     * </ul>
     *
     * @param fotoBase64 Captura de cámara en formato Base64 (puede ser null).
     * @param ipUsuario  IP del cliente para validación de ubicación.
     * @throws IllegalStateException si ya existe una entrada registrada hoy.
     * @throws SecurityException     si la IP no coincide con la del área.
     */
    @Transactional
    public void registrarEntrada(String fotoBase64, String ipUsuario) {
        String numeroControl = securityUtil.getCurrentUser()
            .map(Usuario::getNumeroControl)
            .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        Usuario currentUser = usuarioRepository.findByNumeroControl(numeroControl)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado en la base de datos"));

        LocalDate hoy = LocalDate.now();
        LocalDateTime ahoraCompleto = LocalDateTime.now(); // Usamos LocalDateTime para precisión de segundos

        Optional<Asistencia> asistenciaHoy = asistenciaRepository.findByUsuarioAndFecha(currentUser, hoy);
        if (asistenciaHoy.isPresent() && asistenciaHoy.get().getHoraEntrada() != null) {
            throw new IllegalStateException("Ya tienes un registro de entrada para el día de hoy.");
        }

        Area areaUsuario = currentUser.getAreaPrincipal();
        String ipPermitida = areaUsuario.getIpPermitida();
        if (ipPermitida != null && !ipPermitida.isBlank() && !ipPermitida.equals(ipUsuario)) {
             throw new SecurityException("Estás intentando registrar asistencia desde una ubicación (IP) no autorizada.");
        }

        int estatusIncidencia = ESTATUS_OK; 
        HorarioDetalle turnoDeHoy = calcularTurnoParaFecha(currentUser, hoy);

        if (turnoDeHoy != null) {
            // Unimos la fecha de hoy con su hora de entrada esperada
            LocalDateTime fechaHoraEsperada = LocalDateTime.of(hoy, turnoDeHoy.getHoraEntrada());
            
            // Tolerancia: hasta +15:59 min = OK; hasta +30:59 min = Retardo; más = Falta Total.
            LocalDateTime limiteOk = fechaHoraEsperada.plusMinutes(15).plusSeconds(59);
            LocalDateTime limiteRetardo = fechaHoraEsperada.plusMinutes(30).plusSeconds(59);

            // Validamos en cascada inversa
            if (ahoraCompleto.isAfter(limiteRetardo)) {
                estatusIncidencia = ESTATUS_FALTA_TOTAL; // Llegó después de 30:59 mins = Falta
            } else if (ahoraCompleto.isAfter(limiteOk)) {
                estatusIncidencia = ESTATUS_RETARDO; // Llegó entre 16:00 y 30:59 mins = Retardo
            }
        }

        Asistencia nuevaAsistencia = asistenciaHoy.orElse(new Asistencia());
        nuevaAsistencia.setUsuario(currentUser);
        nuevaAsistencia.setFecha(hoy);
        nuevaAsistencia.setHoraEntrada(ahoraCompleto);
        nuevaAsistencia.setFotoEntrada(fotoBase64);
        nuevaAsistencia.setIpRegistro(ipUsuario);
        nuevaAsistencia.setEstatusIncidencia(estatusIncidencia);
        
        asistenciaRepository.save(nuevaAsistencia);
    }

    /**
     * Registra la salida del usuario autenticado.
     * Soporta turnos con cruce de medianoche: si no hay entrada hoy, busca en
     * la asistencia de ayer siempre que el horario de ayer tenga
     * {@code cruceMedianoche = true} y la salida aún no esté registrada.
     *
     * @param fotoBase64 Captura de cámara (puede ser null).
     * @param ipUsuario  IP del cliente.
     * @throws IllegalStateException si no hay entrada previa o ya se registró salida.
     */
    @Transactional
    public void registrarSalida(String fotoBase64, String ipUsuario) {
        String numeroControl = securityUtil.getCurrentUser()
            .map(Usuario::getNumeroControl)
            .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        Usuario currentUser = usuarioRepository.findByNumeroControl(numeroControl)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado en la base de datos"));

        LocalDate hoy = LocalDate.now();
        Asistencia asistenciaParaSalida = null;

        Optional<Asistencia> busquedaHoy = asistenciaRepository.findByUsuarioAndFecha(currentUser, hoy);
        
        if (busquedaHoy.isPresent() && busquedaHoy.get().getHoraEntrada() != null) {
            asistenciaParaSalida = busquedaHoy.get();
        } 
        else {
            LocalDate ayer = hoy.minusDays(1);
            HorarioDetalle turnoDeAyer = calcularTurnoParaFecha(currentUser, ayer);
            
            if (turnoDeAyer != null && turnoDeAyer.getHorario().getCruceMedianoche()) {
                Optional<Asistencia> busquedaAyer = asistenciaRepository.findByUsuarioAndFecha(currentUser, ayer);
                if (busquedaAyer.isPresent() && busquedaAyer.get().getHoraSalida() == null) {
                    asistenciaParaSalida = busquedaAyer.get();
                }
            }
        }

        if (asistenciaParaSalida == null) {
            throw new IllegalStateException("Debe registrar una entrada antes de poder registrar su salida.");
        }
        if (asistenciaParaSalida.getHoraSalida() != null) {
            throw new IllegalStateException("Ya ha registrado una salida para este turno.");
        }

        asistenciaParaSalida.setHoraSalida(LocalDateTime.now());
        asistenciaParaSalida.setFotoSalida(fotoBase64); 
        asistenciaParaSalida.setIpRegistro(ipUsuario); 

        asistenciaRepository.save(asistenciaParaSalida);
    }

    /**
     * Determina el {@link HorarioDetalle} vigente para un usuario en una fecha dada.
     * Consulta primero excepciones individuales; si hay una excepción de día no laborable,
     * retorna {@code null}. Para horarios semanales usa el día ISO; para ciclos rotativos
     * calcula el día del ciclo desde {@code fechaInicioCiclo}.
     *
     * @param usuario        Usuario a evaluar.
     * @param fechaRequerida Fecha para la que se busca el turno.
     * @return El {@link HorarioDetalle} del turno, o {@code null} si no labora ese día.
     */
    private HorarioDetalle calcularTurnoParaFecha(Usuario usuario, LocalDate fechaRequerida) {
        Optional<ExcepcionHorario> excepcion = excepcionHorarioRepository.findByUsuarioIdAndFechaEspecifica(usuario.getId(), fechaRequerida);
        if (excepcion.isPresent()) {
            if (!excepcion.get().getLabora()) {
                return null; 
            }
        }

        Optional<HorarioUsuario> asignacionOpt = horarioUsuarioRepository.findByUsuarioId(usuario.getId());
        if (asignacionOpt.isEmpty()) {
            return null; 
        }
        
        HorarioUsuario asignacion = asignacionOpt.get();
        Horario horario = asignacion.getHorario();
        List<HorarioDetalle> detalles = horario.getDetalles();
        
        if (detalles.isEmpty()) return null;

        int diaActivo = 0;

        if (horario.getTipoCiclo() == 1) {
            diaActivo = fechaRequerida.getDayOfWeek().getValue();
        } 
        else if (horario.getTipoCiclo() == 2 && asignacion.getFechaInicioCiclo() != null) {
            LocalDate fechaPivote = asignacion.getFechaInicioCiclo();
            
            if (fechaRequerida.isBefore(fechaPivote)) {
                return null; 
            }

            int longitudDelCiclo = detalles.stream().mapToInt(HorarioDetalle::getDia).max().orElse(1);
            long diasTranscurridos = ChronoUnit.DAYS.between(fechaPivote, fechaRequerida);
            diaActivo = (int) (diasTranscurridos % longitudDelCiclo) + 1;
        }

        final int diaBuscado = diaActivo;
        return detalles.stream()
                .filter(d -> d.getDia() == diaBuscado)
                .findFirst()
                .orElse(null); 
    }

    /* ====================================================================================
     * REPORTES Y CONSULTAS PARA ADMINISTRADORES
     * ==================================================================================== */
    @Transactional(readOnly = true)
    public Page<AsistenciaReporteRecord> getReporteAsistencias(
        Optional<LocalDate> fechaInicio, Optional<LocalDate> fechaFin,
        Optional<Integer> usuarioId, Optional<Integer> areaId, Optional<String> key,
        Pageable pageable
    ) {
        Specification<Asistencia> spec = createAsistenciaSpecification(fechaInicio, fechaFin, usuarioId, areaId, key, Optional.of(false)); 
        return asistenciaRepository.findAll(spec, pageable).map(this::toReporteRecord);
    }

    /** Rango máximo permitido en exportaciones para acotar el uso de memoria (PERF-004). */
    private static final long MAX_DIAS_EXPORTACION = 92;

    /**
     * Valida que el rango de fechas no exceda el máximo permitido en exportaciones,
     * que cargan todos los registros del periodo en memoria sin paginar.
     */
    private void validarRangoExportacion(Optional<LocalDate> inicio, Optional<LocalDate> fin) {
        if (inicio.isPresent() && fin.isPresent()) {
            long dias = ChronoUnit.DAYS.between(inicio.get(), fin.get());
            if (dias < 0) {
                throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha fin.");
            }
            if (dias > MAX_DIAS_EXPORTACION) {
                throw new IllegalArgumentException(
                    "El rango no puede exceder " + MAX_DIAS_EXPORTACION + " días (aprox. 3 meses).");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AsistenciaReporteRecord> getReporteData(
        Optional<LocalDate> fechaInicio, Optional<LocalDate> fechaFin,
        Optional<Integer> usuarioId, Optional<Integer> areaId,
        Optional<String> key, Optional<Boolean> soloRetardos
    ) {
        validarRangoExportacion(fechaInicio, fechaFin);
        Specification<Asistencia> spec = createAsistenciaSpecification(fechaInicio, fechaFin, usuarioId, areaId, key, soloRetardos);
        List<Asistencia> asistencias = asistenciaRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "fecha"));
        return asistencias.stream().map(this::toReporteRecord).collect(Collectors.toList());
    }

    private Specification<Asistencia> createAsistenciaSpecification(
        Optional<LocalDate> fechaInicio, Optional<LocalDate> fechaFin,
        Optional<Integer> usuarioId, Optional<Integer> areaId,
        Optional<String> key, Optional<Boolean> soloRetardos
    ) {
        Usuario currentUser = securityUtil.getCurrentUser().orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Asistencia, Usuario> usuarioJoin = root.join("usuario");

            fechaInicio.ifPresent(fecha -> predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fecha"), fecha)));
            fechaFin.ifPresent(fecha -> predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fecha"), fecha)));
            usuarioId.ifPresent(id -> predicates.add(criteriaBuilder.equal(usuarioJoin.get("id"), id)));

            key.ifPresent(searchTerm -> {
                if (!searchTerm.isBlank()) {
                    String pattern = "%" + searchTerm.toLowerCase() + "%";
                    Join<Usuario, Area> areaJoin = usuarioJoin.join("areaPrincipal");
                    predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(usuarioJoin.get("numeroControl")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(usuarioJoin.get("nombre")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(usuarioJoin.get("apellidoPaterno")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(usuarioJoin.get("apellidoMaterno")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(areaJoin.get("nombre")), pattern)
                    ));
                }
            });

            soloRetardos.ifPresent(retardos -> {
                if (retardos) {
                    predicates.add(criteriaBuilder.equal(root.get("estatusIncidencia"), ESTATUS_RETARDO));
                }
            });

            if (currentUser.getRol() == Rol.SUPERADMIN) {
                areaId.ifPresent(id -> predicates.add(criteriaBuilder.equal(usuarioJoin.get("areaPrincipal").get("id"), id)));
            } else if (currentUser.getRol() == Rol.ADMIN) {
                Set<Integer> idsDeSusAreas = areaService.obtenerIdsDeAreasGestionadasPorAdmin(currentUser);
                if (idsDeSusAreas.isEmpty()) return criteriaBuilder.disjunction();
                predicates.add(usuarioJoin.get("areaPrincipal").get("id").in(idsDeSusAreas));            
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /* ====================================================================================
     * MÓDULO MANUAL PARA RECURSOS HUMANOS
     * ==================================================================================== */
    /**
     * Verifica que el administrador autenticado tenga permiso sobre el área del
     * usuario dueño de la asistencia. SUPERADMIN siempre pasa; ADMIN solo si el
     * área pertenece a las que gestiona. Previene IDOR en el módulo manual y de
     * justificaciones (ID-004).
     *
     * @param areaUsuarioId Id del área principal del usuario afectado.
     */
    private void validarAccesoPorArea(Integer areaUsuarioId) {
        Usuario currentUser = securityUtil.getCurrentUser()
            .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
        if (currentUser.getRol() == Rol.ADMIN) {
            Set<Integer> idsPermitidos = areaService.obtenerIdsDeAreasGestionadasPorAdmin(currentUser);
            if (!idsPermitidos.contains(areaUsuarioId)) {
                throw new SecurityException("No tiene permiso para operar sobre asistencias de esta área.");
            }
        }
    }

   @Transactional
    public AsistenciaReporteRecord createManual(AsistenciaManualRecord record) {
        Usuario usuario = usuarioRepository.findById(record.usuarioId())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        validarAccesoPorArea(usuario.getAreaPrincipal().getId());

        Asistencia entity = new Asistencia();
        entity.setUsuario(usuario);
        entity.setFecha(record.fecha());
        entity.setHoraEntrada(record.horaEntrada());
        entity.setHoraSalida(record.horaSalida());
        
        entity.setEstatusIncidencia(record.estatusIncidencia() != null ? record.estatusIncidencia() : ESTATUS_OK);

        entity = asistenciaRepository.save(entity);
        return toReporteRecord(entity);
    }

    @Transactional
    public AsistenciaReporteRecord updateManual(Long id, AsistenciaManualRecord record) {
        Asistencia entity = asistenciaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Registro de asistencia no encontrado"));

        Usuario usuario = usuarioRepository.findById(record.usuarioId())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Valida tanto el dueño actual del registro como el destino del cambio.
        validarAccesoPorArea(entity.getUsuario().getAreaPrincipal().getId());
        validarAccesoPorArea(usuario.getAreaPrincipal().getId());

        entity.setUsuario(usuario);
        entity.setFecha(record.fecha());
        entity.setHoraEntrada(record.horaEntrada());
        entity.setHoraSalida(record.horaSalida());
        
        entity.setEstatusIncidencia(record.estatusIncidencia() != null ? record.estatusIncidencia() : ESTATUS_OK);

        entity = asistenciaRepository.save(entity);
        return toReporteRecord(entity);
    }

    @Transactional
    public void deleteById(Long id) {
        Asistencia entity = asistenciaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Registro de asistencia no encontrado para eliminar"));
        validarAccesoPorArea(entity.getUsuario().getAreaPrincipal().getId());
        asistenciaRepository.deleteById(id);
    }

    @Transactional(readOnly = true) 
    public Map<String, Boolean> getEstadoAsistenciaDiario(String numeroControl) {
        Usuario usuario = usuarioRepository.findByNumeroControl(numeroControl)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        Optional<Asistencia> asistenciaHoy = asistenciaRepository.findByUsuarioAndFecha(usuario, LocalDate.now());

        boolean entradaRegistrada = asistenciaHoy.map(a -> a.getHoraEntrada() != null).orElse(false);
        boolean salidaRegistrada = asistenciaHoy.map(a -> a.getHoraSalida() != null).orElse(false);

        Map<String, Boolean> estado = new HashMap<>();
        estado.put("entradaRegistrada", entradaRegistrada);
        estado.put("salidaRegistrada", salidaRegistrada);
        return estado;
    }

    private AsistenciaReporteRecord toReporteRecord(Asistencia entity) {
        Usuario usuario = entity.getUsuario();
        Area area = usuario.getAreaPrincipal();
        String nombreCompleto = usuario.getNombre() + " " + usuario.getApellidoPaterno() +
                (usuario.getApellidoMaterno() != null ? " " + usuario.getApellidoMaterno() : "");

        String motivo = null;
        String estatusJustificacion = null;
        
        if (entity.getJustificacionAplicada() != null) {
            motivo = entity.getJustificacionAplicada().getJustificacion().getNombre();
            estatusJustificacion = entity.getJustificacionAplicada().getEstatus().name();
        }

        return new AsistenciaReporteRecord(
                entity.getId(),
                entity.getFecha(),
                entity.getHoraEntrada(),
                entity.getHoraSalida(),
                entity.getEstatusIncidencia(),
                usuario.getId(),
                usuario.getNumeroControl(),
                nombreCompleto.trim(),
                area.getId(),
                area.getNombre(),
                // Las fotos NO viajan en los listados/reportes para no transferir
                // megabytes por página (PERF-002/ID-011). Se obtienen on-demand vía
                // GET /api/asistencia/{id}/fotos al abrir el detalle.
                null,
                null,
                entity.getIpRegistro(),
                motivo,
                estatusJustificacion
        );
    }

    /**
     * Devuelve las fotos (entrada/salida) de una asistencia bajo demanda.
     * Valida que el usuario autenticado tenga acceso: el propio dueño del registro,
     * o un admin con permiso sobre el área del usuario (ID-004/ID-011).
     *
     * @param asistenciaId Id del registro de asistencia.
     * @return Mapa con claves "fotoEntrada" y "fotoSalida" (pueden ser null).
     */
    @Transactional(readOnly = true)
    public Map<String, String> obtenerFotos(Long asistenciaId) {
        Asistencia asistencia = asistenciaRepository.findById(asistenciaId)
            .orElseThrow(() -> new RuntimeException("Registro de asistencia no encontrado"));

        Usuario currentUser = securityUtil.getCurrentUser()
            .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        boolean esDueno = asistencia.getUsuario().getNumeroControl().equals(currentUser.getNumeroControl());
        if (!esDueno) {
            // No es su propia asistencia: debe ser admin con permiso sobre el área.
            validarAccesoPorArea(asistencia.getUsuario().getAreaPrincipal().getId());
        }

        Map<String, String> fotos = new HashMap<>();
        fotos.put("fotoEntrada", asistencia.getFotoEntrada());
        fotos.put("fotoSalida", asistencia.getFotoSalida());
        return fotos;
    }

    @Transactional
    public Map<String, Object> procesarCargaMasivaExcel(MultipartFile file) throws Exception {
        int procesados = 0;
        int errores = 0;
        List<String> detalleErrores = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0); 

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; 

                try {
                    String numeroControl = getCellValueAsString(row.getCell(0));
                    if (numeroControl.isBlank()) continue;

                    LocalDate fecha = row.getCell(1).getLocalDateTimeCellValue().toLocalDate();
                    
                    LocalDateTime horaEntrada = null;
                    if (row.getCell(2) != null && row.getCell(2).getCellType() != CellType.BLANK) {
                        horaEntrada = LocalDateTime.of(fecha, row.getCell(2).getLocalDateTimeCellValue().toLocalTime());
                    }

                    LocalDateTime horaSalida = null;
                    if (row.getCell(3) != null && row.getCell(3).getCellType() != CellType.BLANK) {
                        horaSalida = LocalDateTime.of(fecha, row.getCell(3).getLocalDateTimeCellValue().toLocalTime());
                    }

                    Usuario usuario = usuarioRepository.findByNumeroControl(numeroControl)
                        .orElseThrow(() -> new RuntimeException("Usuario no existe: " + numeroControl));

                    int estatusIncidencia = ESTATUS_OK;
                    HorarioDetalle turno = calcularTurnoParaFecha(usuario, fecha);

                    if (turno != null) {
                        // Caso A: No vino a trabajar (Celdas vacías)
                        if (horaEntrada == null && horaSalida == null) {
                            estatusIncidencia = ESTATUS_FALTA_TOTAL;
                        } 
                        // Caso B: Olvidó checar entrada, pero sí tiene salida
                        else if (horaEntrada == null && horaSalida != null) {
                            estatusIncidencia = ESTATUS_OMISION_ENTRADA;
                        } 
                        // Caso C: Checó entrada, pero olvidó su salida
                        else if (horaEntrada != null && horaSalida == null) {
                            estatusIncidencia = ESTATUS_OMISION_SALIDA;
                        } 
                        // Caso D: ambos registros presentes — se evalúa puntualidad y salida completa.
                        else if (horaEntrada != null && horaSalida != null) {
                            LocalTime salidaOficial = turno.getHoraSalida();
                            LocalTime salidaReal = horaSalida.toLocalTime();

                            // Salió antes de su hora oficial → Falta Total independientemente de la entrada.
                            if (salidaReal.isBefore(salidaOficial)) {
                                estatusIncidencia = ESTATUS_FALTA_TOTAL;
                            } else {
                                // Cumplió salida: evaluamos la llegada con la misma tolerancia que el registro en línea.
                                LocalDateTime fechaHoraEsperada = LocalDateTime.of(fecha, turno.getHoraEntrada());
                                LocalDateTime limiteOk = fechaHoraEsperada.plusMinutes(15).plusSeconds(59);
                                LocalDateTime limiteRetardo = fechaHoraEsperada.plusMinutes(30).plusSeconds(59);

                                if (horaEntrada.isAfter(limiteRetardo)) {
                                    estatusIncidencia = ESTATUS_FALTA_TOTAL;
                                } else if (horaEntrada.isAfter(limiteOk)) {
                                    estatusIncidencia = ESTATUS_RETARDO;
                                }
                            }
                        }
                    }

                    Asistencia asistencia = asistenciaRepository.findByUsuarioAndFecha(usuario, fecha)
                        .orElse(new Asistencia());
                    
                    asistencia.setUsuario(usuario);
                    asistencia.setFecha(fecha);
                    asistencia.setHoraEntrada(horaEntrada);
                    asistencia.setHoraSalida(horaSalida);
                    asistencia.setEstatusIncidencia(estatusIncidencia);
                    asistencia.setIpRegistro("CARGA_MASIVA_EXCEL");
                    
                    asistenciaRepository.save(asistencia);
                    procesados++;

                } catch (Exception e) {
                    errores++;
                    detalleErrores.add("Fila " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }
        }
        return Map.of(
            "procesados", procesados, 
            "errores", errores, 
            "detalleErrores", detalleErrores
        );
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return "";
    }

    /* ====================================================================================
     * CÁLCULO DE SANCIONES GLOBALES (RETARDOS, FALTAS Y OMISIONES)
     * ==================================================================================== */
    @Transactional(readOnly = true)
    public List<ResumenSancionesRecord> calcularSanciones(LocalDate fechaInicio, LocalDate fechaFin, Optional<Integer> usuarioId, Optional<Integer> areaId) {
        validarRangoExportacion(Optional.ofNullable(fechaInicio), Optional.ofNullable(fechaFin));

        // 1. Traemos TODAS las asistencias del periodo
        Specification<Asistencia> spec = createAsistenciaSpecification(
            Optional.of(fechaInicio), Optional.of(fechaFin),
            usuarioId, areaId, Optional.empty(), Optional.empty()
        );
        List<Asistencia> todasLasAsistencias = asistenciaRepository.findAll(spec);

        // 2. Filtramos: Solo nos importan las que son Incidencias (> 0) y NO están justificadas
        List<Asistencia> incidenciasEfectivas = todasLasAsistencias.stream()
            .filter(a -> a.getEstatusIncidencia() > ESTATUS_OK)
            .filter(a -> a.getJustificacionAplicada() == null ||
                        a.getJustificacionAplicada().getEstatus() != EstatusJustificacion.APROBADA)
            .collect(Collectors.toList());

        // 3. Agrupamos todas las incidencias por Empleado
        Map<Usuario, List<Asistencia>> incidenciasPorUsuario = incidenciasEfectivas.stream()
            .collect(Collectors.groupingBy(Asistencia::getUsuario));

        // 4. Procesamos a cada empleado para calcular sus días de castigo y extraer sus fechas
        return incidenciasPorUsuario.entrySet().stream()
            .map(entry -> {
                Usuario u = entry.getKey();
                List<Asistencia> asists = entry.getValue();

                // Separar Retardos
                List<Asistencia> retardos = asists.stream()
                        .filter(a -> a.getEstatusIncidencia() == ESTATUS_RETARDO).toList();
                long totalRetardos = retardos.size();
                double descuentoRetardos = aplicarReglaDescuento(totalRetardos);
                List<LocalDate> fechasRetardos = retardos.stream()
                        .map(Asistencia::getFecha).sorted().toList();

                // Separar Faltas y Omisiones (Valen 1 día entero cada una)
                List<Asistencia> faltasYOmisiones = asists.stream()
                        .filter(a -> a.getEstatusIncidencia() == ESTATUS_FALTA_TOTAL || 
                                     a.getEstatusIncidencia() == ESTATUS_OMISION_ENTRADA || 
                                     a.getEstatusIncidencia() == ESTATUS_OMISION_SALIDA).toList();
                long totalFaltas = faltasYOmisiones.size();
                double descuentoFaltas = totalFaltas * 1.0; // 1 Falta/Omisión = 1 día de descuento
                List<LocalDate> fechasFaltas = faltasYOmisiones.stream()
                        .map(Asistencia::getFecha).sorted().toList();

                String nombreCompleto = u.getNombre() + " " + u.getApellidoPaterno() + 
                                       (u.getApellidoMaterno() != null ? " " + u.getApellidoMaterno() : "");
                
                return new ResumenSancionesRecord(
                    u.getNumeroControl(),
                    nombreCompleto.trim(),
                    u.getAreaPrincipal().getNombre(),
                    totalRetardos,
                    descuentoRetardos,
                    fechasRetardos,
                    totalFaltas,
                    descuentoFaltas,
                    fechasFaltas,
                    descuentoRetardos + descuentoFaltas
                );
            })
            // Ordenamos alfabéticamente por área y luego por nombre
            .sorted(Comparator.comparing(ResumenSancionesRecord::area).thenComparing(ResumenSancionesRecord::nombreCompleto))
            .collect(Collectors.toList());
    }

    private double aplicarReglaDescuento(long retardos) {
        if (retardos < 4) return 0.0;
        if (retardos == 4) return 0.5;
        if (retardos >= 5 && retardos <= 8) return 1.0;
        if (retardos == 9) return 1.5;
        return 2.0; // 10 o más retardos
    }

    /* ====================================================================================
     * AUTOMATIZACIÓN NOCTURNA: CÁLCULO DE FALTAS Y OMISIONES
     * ==================================================================================== */

    /** Número de usuarios procesados por lote en el job nocturno. */
    private static final int BATCH_SIZE_NOCTURNO = 100;

    /**
     * Cierra las incidencias del día procesando a los usuarios en lotes de
     * {@value #BATCH_SIZE_NOCTURNO}. Lo dispara {@link AsistenciaNocturnaJob}, que
     * vive en otro bean para que la anotación {@link Transactional} de
     * {@link #procesarLote} se aplique vía proxy (evita el bug de self-invocation).
     *
     * <p>Por cada usuario con horario activo:
     * <ul>
     *   <li>Si no registró nada → crea una Falta Total.</li>
     *   <li>Si registró entrada pero no salida → marca Omisión de Salida.</li>
     *   <li>Si registró salida pero no entrada → marca Omisión de Entrada.</li>
     * </ul>
     */
    /** Tamaño de página usado por el job nocturno para iterar usuarios. */
    public static int getBatchSizeNocturno() {
        return BATCH_SIZE_NOCTURNO;
    }

    /** Devuelve una página de usuarios para el procesamiento nocturno por lotes. */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Usuario> obtenerPaginaUsuarios(int pageNumber) {
        return usuarioRepository.findAll(
                org.springframework.data.domain.PageRequest.of(pageNumber, BATCH_SIZE_NOCTURNO));
    }

    /**
     * Procesa un lote de usuarios evaluando sus incidencias del día dado.
     * Se ejecuta en una transacción propia para que cada lote se confirme de
     * forma independiente y no se mantenga un contexto JPA enorme en memoria.
     *
     * @param usuarios Lista de usuarios del lote actual.
     * @param fecha    Fecha a procesar (normalmente el día actual).
     */
    @Transactional
    public void procesarLote(List<Usuario> usuarios, LocalDate fecha) {
        for (Usuario usuario : usuarios) {
            HorarioDetalle turnoHoy = calcularTurnoParaFecha(usuario, fecha);
            if (turnoHoy == null) continue;

            Optional<Asistencia> registroOpt = asistenciaRepository.findByUsuarioAndFecha(usuario, fecha);

            if (registroOpt.isEmpty()) {
                Asistencia nuevaFalta = new Asistencia();
                nuevaFalta.setUsuario(usuario);
                nuevaFalta.setFecha(fecha);
                nuevaFalta.setEstatusIncidencia(ESTATUS_FALTA_TOTAL);
                asistenciaRepository.save(nuevaFalta);
            } else {
                Asistencia registro = registroOpt.get();
                if (registro.getHoraEntrada() != null && registro.getHoraSalida() == null) {
                    registro.setEstatusIncidencia(ESTATUS_OMISION_SALIDA);
                    asistenciaRepository.save(registro);
                } else if (registro.getHoraEntrada() == null && registro.getHoraSalida() != null) {
                    registro.setEstatusIncidencia(ESTATUS_OMISION_ENTRADA);
                    asistenciaRepository.save(registro);
                }
            }
        }
    }

    /* ====================================================================================
     * APLICACIÓN DE JUSTIFICACIONES (MANTIENE EL ESTATUS ORIGINAL INTACTO)
     * ==================================================================================== */
    /**
     * Registra (o re-registra) una justificación sobre una incidencia de asistencia.
     *
     * <p>El estatus inicial define el flujo:
     * <ul>
     *   <li>{@code PENDIENTE} → la envía el empleado y queda esperando aprobación del admin.</li>
     *   <li>{@code APROBADA}  → atajo del admin: justifica y aprueba en un solo paso.</li>
     * </ul>
     *
     * <p>Reglas de reintento: si ya existe una justificación previa, solo se permite
     * sobrescribirla cuando estaba {@code RECHAZADA} (el empleado puede volver a intentar).
     * Si está {@code PENDIENTE} o {@code APROBADA}, se bloquea para no duplicar el trámite.
     *
     * @param asistenciaId    Id de la asistencia a justificar.
     * @param justificacionId Id del motivo del catálogo.
     * @param observacion     Texto opcional/obligatorio según el motivo.
     * @param usuarioRegistro Número de control de quien registra la justificación.
     * @param estatusInicial  Estatus con el que nace la justificación (PENDIENTE o APROBADA).
     */
    @Transactional
    public void justificarAsistencia(Long asistenciaId, Integer justificacionId, String observacion, String usuarioRegistro, EstatusJustificacion estatusInicial) {
        // 1. Buscamos el registro de asistencia
        Asistencia asistencia = asistenciaRepository.findById(asistenciaId)
            .orElseThrow(() -> new RuntimeException("Registro de asistencia no encontrado"));

        // 1.b Un ADMIN solo puede justificar asistencias de sus áreas (ID-004).
        //     Para el flujo del empleado (justificarMiAsistencia) el currentUser es
        //     USER y validarAccesoPorArea no aplica restricción, pero la propiedad
        //     ya se verificó en justificarMiAsistencia antes de delegar aquí.
        validarAccesoPorArea(asistencia.getUsuario().getAreaPrincipal().getId());

        // 2. Validamos el estado de cualquier justificación previa.
        //    Consultamos por id de asistencia (fuente de verdad) en lugar de la
        //    relación lazy inversa, para evitar lecturas cacheadas/inconsistentes.
        //    Solo se permite continuar si NO existe o si la anterior fue RECHAZADA (reintento).
        AsistenciaJustificacion justificacionPrevia =
                asistenciaJustificacionRepository.findByAsistenciaId(asistenciaId).orElse(null);
        if (justificacionPrevia != null
                && justificacionPrevia.getEstatus() != EstatusJustificacion.RECHAZADA) {
            throw new IllegalStateException("Este registro ya tiene una justificación en proceso o aprobada.");
        }

        // 3. Buscamos el motivo en el catálogo
        CatalogoJustificacion justificacion = catalogoJustificacionRepository.findById(justificacionId)
            .orElseThrow(() -> new RuntimeException("El motivo de justificación seleccionado no existe."));

        // 4. Validamos la regla del catálogo: Si exige observación, no puede ir vacía
        if (justificacion.getRequiereObservacion() && (observacion == null || observacion.trim().isEmpty())) {
            throw new IllegalArgumentException("Este motivo de justificación requiere una observación obligatoria.");
        }

        // 5. Reutilizamos la fila rechazada si existe (reintento); si no, creamos una nueva.
        //    Así evitamos duplicar registros y respetamos la restricción @OneToOne unique.
        AsistenciaJustificacion justificacionAGuardar =
                (justificacionPrevia != null) ? justificacionPrevia : new AsistenciaJustificacion();

        justificacionAGuardar.setAsistencia(asistencia);
        justificacionAGuardar.setJustificacion(justificacion);
        justificacionAGuardar.setObservacion(observacion);
        justificacionAGuardar.setUsuarioRegistro(usuarioRegistro); // Quien registra (empleado o admin)
        justificacionAGuardar.setEstatus(estatusInicial);          // PENDIENTE (empleado) o APROBADA (atajo admin)

        asistenciaJustificacionRepository.save(justificacionAGuardar);
    }

    /* ====================================================================================
     * PORTAL DEL EMPLEADO (VISTAS Y ACCIONES PROPIAS)
     * ==================================================================================== */

    /**
     * Retorna el historial paginado de asistencias del empleado identificado por
     * {@code numeroControl}, sin restricción de fechas.
     */
   @Transactional(readOnly = true)
    public Page<AsistenciaReporteRecord> getMisAsistencias(String numeroControl, Pageable pageable) {
        Usuario usuario = usuarioRepository.findByNumeroControl(numeroControl)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Specification<Asistencia> spec = (root, query, cb) ->
            cb.equal(root.get("usuario").get("id"), usuario.getId());

        return asistenciaRepository.findAll(spec, pageable).map(this::toReporteRecord);
    }

    @Transactional
    public void justificarMiAsistencia(Long asistenciaId, Integer justificacionId, String observacion, String numeroControl) {
        Asistencia asistencia = asistenciaRepository.findById(asistenciaId)
            .orElseThrow(() -> new RuntimeException("Registro de asistencia no encontrado"));

        // Previene que un usuario justifique incidencias ajenas usando su propio token.
        if (!asistencia.getUsuario().getNumeroControl().equals(numeroControl)) {
            throw new SecurityException("No tienes permiso para justificar una asistencia que no es tuya.");
        }

        // Si pasa el filtro de seguridad, reutilizamos la lógica central que ya programaste
        justificarAsistencia(asistenciaId, justificacionId, observacion, numeroControl, EstatusJustificacion.PENDIENTE);
    }

    /**
     * Aprueba una justificación que está en estado PENDIENTE.
     * Una vez aprobada, la incidencia deja de contar para el cálculo de sanciones.
     *
     * @param asistenciaId Id de la asistencia cuya justificación se aprueba.
     * @throws IllegalStateException si no hay justificación o si no está PENDIENTE.
     */
    @Transactional
    public void aprobarJustificacion(Long asistenciaId) {
        AsistenciaJustificacion justificacion = obtenerJustificacionPendiente(asistenciaId);
        justificacion.setEstatus(EstatusJustificacion.APROBADA);
        asistenciaJustificacionRepository.save(justificacion);
    }

    /**
     * Rechaza una justificación que está en estado PENDIENTE.
     * Tras el rechazo, el empleado puede volver a justificar la misma incidencia.
     *
     * @param asistenciaId Id de la asistencia cuya justificación se rechaza.
     * @throws IllegalStateException si no hay justificación o si no está PENDIENTE.
     */
    @Transactional
    public void rechazarJustificacion(Long asistenciaId) {
        AsistenciaJustificacion justificacion = obtenerJustificacionPendiente(asistenciaId);
        justificacion.setEstatus(EstatusJustificacion.RECHAZADA);
        asistenciaJustificacionRepository.save(justificacion);
    }

    /**
     * Recupera la justificación de una asistencia validando que exista y que
     * esté en estado PENDIENTE. Centraliza la regla compartida por aprobar/rechazar
     * para impedir transiciones inválidas (ej. re-aprobar una ya rechazada).
     *
     * @param asistenciaId Id de la asistencia.
     * @return La justificación pendiente lista para cambiar de estado.
     * @throws IllegalStateException si no existe o no está PENDIENTE.
     */
    private AsistenciaJustificacion obtenerJustificacionPendiente(Long asistenciaId) {
        // Consultamos directamente por id de asistencia (fuente de verdad determinista),
        // en vez de la relación lazy inversa Asistencia.justificacionAplicada.
        AsistenciaJustificacion justificacion =
                asistenciaJustificacionRepository.findByAsistenciaId(asistenciaId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Esta incidencia no tiene una justificación registrada."));

        // Un ADMIN solo puede aprobar/rechazar justificaciones de sus áreas (ID-004).
        validarAccesoPorArea(justificacion.getAsistencia().getUsuario().getAreaPrincipal().getId());

        if (justificacion.getEstatus() != EstatusJustificacion.PENDIENTE) {
            throw new IllegalStateException("Esta justificación ya fue procesada y no puede modificarse.");
        }
        return justificacion;
    }
}