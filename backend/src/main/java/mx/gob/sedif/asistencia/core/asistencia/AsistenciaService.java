package mx.gob.sedif.asistencia.core.asistencia;

import lombok.RequiredArgsConstructor;
import mx.gob.sedif.asistencia.core.area.Area;
import mx.gob.sedif.asistencia.core.area.AreaService;
import mx.gob.sedif.asistencia.core.horario.*;
import mx.gob.sedif.asistencia.core.usuario.Usuario;
import mx.gob.sedif.asistencia.core.usuario.UsuarioRepository;
import mx.gob.sedif.asistencia.security.SecurityUtil;
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

@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final HorarioUsuarioRepository horarioUsuarioRepository;
    private final ExcepcionHorarioRepository excepcionHorarioRepository;
    private final SecurityUtil securityUtil;
    private final UsuarioRepository usuarioRepository;
    private final AreaService areaService;

    // Constantes del Catálogo de Incidencias
    private static final int ESTATUS_OK = 0;
    private static final int ESTATUS_RETARDO = 1;
    private static final int ESTATUS_FALTA_TOTAL = 2;
    private static final int ESTATUS_OMISION_ENTRADA = 3;
    private static final int ESTATUS_OMISION_SALIDA = 4;

    /* ====================================================================================
     * REGLA 1: REGISTRO DE ENTRADA Y CÁLCULO DE RETARDOS
     * ==================================================================================== */
    @Transactional
    public void registrarEntrada(String fotoBase64, String ipUsuario) {
        String numeroControl = securityUtil.getCurrentUser()
            .map(Usuario::getNumeroControl)
            .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        Usuario currentUser = usuarioRepository.findByNumeroControl(numeroControl)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado en la base de datos"));

        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();

        // 1.1 Validación de Seguridad: Evitar doble entrada
        Optional<Asistencia> asistenciaHoy = asistenciaRepository.findByUsuarioAndFecha(currentUser, hoy);
        if (asistenciaHoy.isPresent() && asistenciaHoy.get().getHoraEntrada() != null) {
            throw new IllegalStateException("Ya tienes un registro de entrada para el día de hoy.");
        }

        // 1.2 Validación de Red: Bloqueo por IP (Si el área lo exige)
        Area areaUsuario = currentUser.getAreaPrincipal();
        String ipPermitida = areaUsuario.getIpPermitida();
        if (ipPermitida != null && !ipPermitida.isBlank() && !ipPermitida.equals(ipUsuario)) {
             throw new SecurityException("Estás intentando registrar asistencia desde una ubicación (IP) no autorizada.");
        }

        // 1.3 Motor de Incidencias: ¿Llegó tarde?
        int estatusIncidencia = ESTATUS_OK; 
        HorarioDetalle turnoDeHoy = calcularTurnoParaFecha(currentUser, hoy);

        if (turnoDeHoy != null) {
            LocalTime horaEsperada = turnoDeHoy.getHoraEntrada();
            int minutosTolerancia = turnoDeHoy.getToleranciaMinutos();
            LocalTime horaLimiteTolerancia = horaEsperada.plusMinutes(minutosTolerancia);

            // Si la hora actual es posterior a su hora límite, se marca como Retardo
            if (ahora.isAfter(horaLimiteTolerancia)) {
                estatusIncidencia = ESTATUS_RETARDO;
                // NOTA FUTURA: Aquí podrías agregar otra condición. Ej: Si llega 2 horas tarde = Falta Total (2).
            }
        }

        // 1.4 Guardado de la Información
        Asistencia nuevaAsistencia = asistenciaHoy.orElse(new Asistencia());
        nuevaAsistencia.setUsuario(currentUser);
        nuevaAsistencia.setFecha(hoy);
        nuevaAsistencia.setHoraEntrada(LocalDateTime.now());
        nuevaAsistencia.setFotoEntrada(fotoBase64);
        nuevaAsistencia.setIpRegistro(ipUsuario);
        nuevaAsistencia.setEstatusIncidencia(estatusIncidencia);
        
        asistenciaRepository.save(nuevaAsistencia);
    }

    /* ====================================================================================
     * REGLA 2: REGISTRO DE SALIDA Y CRUCE DE MEDIANOCHE
     * ==================================================================================== */
    @Transactional
    public void registrarSalida(String fotoBase64, String ipUsuario) {
        String numeroControl = securityUtil.getCurrentUser()
            .map(Usuario::getNumeroControl)
            .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        Usuario currentUser = usuarioRepository.findByNumeroControl(numeroControl)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado en la base de datos"));

        LocalDate hoy = LocalDate.now();
        Asistencia asistenciaParaSalida = null;

        // 2.1 Búsqueda Estándar: Buscar si tiene entrada el día de hoy
        Optional<Asistencia> busquedaHoy = asistenciaRepository.findByUsuarioAndFecha(currentUser, hoy);
        
        if (busquedaHoy.isPresent() && busquedaHoy.get().getHoraEntrada() != null) {
            asistenciaParaSalida = busquedaHoy.get();
        } 
        // 2.2 Búsqueda de Cruce de Medianoche: Si no hay entrada hoy, buscamos si entró AYER y su turno cruza la medianoche
        else {
            LocalDate ayer = hoy.minusDays(1);
            HorarioDetalle turnoDeAyer = calcularTurnoParaFecha(currentUser, ayer);
            
            if (turnoDeAyer != null && turnoDeAyer.getHorario().getCruceMedianoche()) {
                Optional<Asistencia> busquedaAyer = asistenciaRepository.findByUsuarioAndFecha(currentUser, ayer);
                // Si encontramos un registro de ayer y todavía no tiene salida registrada, lo usamos
                if (busquedaAyer.isPresent() && busquedaAyer.get().getHoraSalida() == null) {
                    asistenciaParaSalida = busquedaAyer.get();
                }
            }
        }

        // 2.3 Validaciones Finales
        if (asistenciaParaSalida == null) {
            throw new IllegalStateException("Debe registrar una entrada antes de poder registrar su salida.");
        }
        if (asistenciaParaSalida.getHoraSalida() != null) {
            throw new IllegalStateException("Ya ha registrado una salida para este turno.");
        }

        // 2.4 Guardado de la Información
        asistenciaParaSalida.setHoraSalida(LocalDateTime.now());
        asistenciaParaSalida.setFotoSalida(fotoBase64); 
        // Solo actualizamos la IP si la salida también requiere validación, o guardamos la última conocida
        asistenciaParaSalida.setIpRegistro(ipUsuario); 

        asistenciaRepository.save(asistenciaParaSalida);
    }

    /* ====================================================================================
     * EL CEREBRO: MOTOR DE CÁLCULO DE TURNOS Y EXCEPCIONES
     * ==================================================================================== */
    private HorarioDetalle calcularTurnoParaFecha(Usuario usuario, LocalDate fechaRequerida) {
        
        // 3.1 Verificamos si Recursos Humanos metió una Excepción Manual (Vacaciones, incapacidad, cambio forzado)
        Optional<ExcepcionHorario> excepcion = excepcionHorarioRepository.findByUsuarioIdAndFechaEspecifica(usuario.getId(), fechaRequerida);
        if (excepcion.isPresent()) {
            if (!excepcion.get().getLabora()) {
                return null; // Si RH dijo que NO labora, devolvemos nulo para que el sistema sepa que es su descanso.
            }
            // Si labora, pero tiene horario especial, la DB actual no tiene campo para "Id Horario Especial",
            // así que asumimos que aplica su horario base pero es obligatorio que asista.
        }

        // 3.2 Obtenemos la asignación del horario del usuario
        Optional<HorarioUsuario> asignacionOpt = horarioUsuarioRepository.findByUsuarioId(usuario.getId());
        if (asignacionOpt.isEmpty()) {
            return null; // No tiene horario asignado
        }
        
        HorarioUsuario asignacion = asignacionOpt.get();
        Horario horario = asignacion.getHorario();
        List<HorarioDetalle> detalles = horario.getDetalles();
        
        if (detalles.isEmpty()) return null;

        int diaActivo = 0;

        // 3.3 CÁLCULO TIPO 1: Semanal Clásico (Lunes a Viernes)
        if (horario.getTipoCiclo() == 1) {
            // getDayOfWeek().getValue() devuelve 1 (Lunes) hasta 7 (Domingo)
            diaActivo = fechaRequerida.getDayOfWeek().getValue();
        } 
        // 3.4 CÁLCULO TIPO 2: Turnos Rotativos (Ej. 1x1, 24x48)
        else if (horario.getTipoCiclo() == 2 && asignacion.getFechaInicioCiclo() != null) {
            LocalDate fechaPivote = asignacion.getFechaInicioCiclo();
            
            // Si la fecha que buscamos es anterior al día que lo contrataron/inició el ciclo, no hay turno
            if (fechaRequerida.isBefore(fechaPivote)) {
                return null; 
            }

            // Calculamos cuántos días de duración tiene el ciclo total buscando el día más alto en los detalles
            // Ej: Un 1x1 tiene un detalle para el día 1. El día 2 es descanso. El ciclo dura 2 días.
            int longitudDelCiclo = detalles.stream().mapToInt(HorarioDetalle::getDia).max().orElse(1);
            
            long diasTranscurridos = ChronoUnit.DAYS.between(fechaPivote, fechaRequerida);
            
            // Matemática modular: Si pasaron 5 días en un ciclo de 2. (5 % 2) = 1. Más 1 = Día 2 del ciclo.
            diaActivo = (int) (diasTranscurridos % longitudDelCiclo) + 1;
        }

        // 3.5 Buscamos en la lista de detalles el día que acabamos de calcular
        final int diaBuscado = diaActivo;
        return detalles.stream()
                .filter(d -> d.getDia() == diaBuscado)
                .findFirst()
                .orElse(null); // Si el día no está en los detalles, significa que hoy es su día de descanso.
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

    @Transactional(readOnly = true)
    public List<AsistenciaReporteRecord> getReporteData(
        Optional<LocalDate> fechaInicio, Optional<LocalDate> fechaFin,
        Optional<Integer> usuarioId, Optional<Integer> areaId,
        Optional<String> key, Optional<Boolean> soloRetardos
    ) {
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
   @Transactional
    public AsistenciaReporteRecord createManual(AsistenciaManualRecord record) {
        Usuario usuario = usuarioRepository.findById(record.usuarioId())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

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
        if (!asistenciaRepository.existsById(id)) {
            throw new RuntimeException("Registro de asistencia no encontrado para eliminar");
        }
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
                entity.getFotoEntrada(),
                entity.getFotoSalida(),
                entity.getIpRegistro()
        );
    }

    @Transactional
    public Map<String, Object> procesarCargaMasivaExcel(MultipartFile file) throws Exception {
        int procesados = 0;
        int errores = 0;
        List<String> detalleErrores = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0); // Leer la primera pestaña del Excel

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Saltar la fila 0 (los encabezados)

                try {
                    // 1. Extraer Número de Control (Columna A)
                    String numeroControl = getCellValueAsString(row.getCell(0));
                    if (numeroControl.isBlank()) continue;

                    // 2. Extraer Fecha (Columna B)
                    LocalDate fecha = row.getCell(1).getLocalDateTimeCellValue().toLocalDate();
                    
                    // 3. Extraer Entrada y Salida (Columnas C y D)
                    LocalDateTime horaEntrada = null;
                    if (row.getCell(2) != null && row.getCell(2).getCellType() != CellType.BLANK) {
                        horaEntrada = LocalDateTime.of(fecha, row.getCell(2).getLocalDateTimeCellValue().toLocalTime());
                    }

                    LocalDateTime horaSalida = null;
                    if (row.getCell(3) != null && row.getCell(3).getCellType() != CellType.BLANK) {
                        horaSalida = LocalDateTime.of(fecha, row.getCell(3).getLocalDateTimeCellValue().toLocalTime());
                    }

                    // 4. Buscar al empleado en la base de datos
                    Usuario usuario = usuarioRepository.findByNumeroControl(numeroControl)
                        .orElseThrow(() -> new RuntimeException("Usuario no existe: " + numeroControl));

                    // 5. ¡El Cerebro Matemático entra en acción!
                    int estatusIncidencia = ESTATUS_OK;
                    HorarioDetalle turno = calcularTurnoParaFecha(usuario, fecha);

                    if (turno != null) {
                        if (horaEntrada != null) {
                            LocalTime limite = turno.getHoraEntrada().plusMinutes(turno.getToleranciaMinutos());
                            if (horaEntrada.toLocalTime().isAfter(limite)) {
                                estatusIncidencia = ESTATUS_RETARDO;
                            }
                        } else {
                            estatusIncidencia = ESTATUS_FALTA_TOTAL; // Tenía turno pero no registró entrada
                        }
                    }

                    // 6. Guardar en la base de datos (Actualiza si ya existe, crea si es nuevo)
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

    // Función auxiliar para evitar que los números de control se lean con decimales (ej. 12345.0)
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return "";
    }
}