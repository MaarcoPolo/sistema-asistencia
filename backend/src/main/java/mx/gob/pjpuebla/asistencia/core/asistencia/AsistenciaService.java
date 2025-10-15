package mx.gob.pjpuebla.asistencia.core.asistencia;

import lombok.RequiredArgsConstructor;
import mx.gob.pjpuebla.asistencia.core.horario.Horario;
import mx.gob.pjpuebla.asistencia.core.horario.HorarioRepository;
import mx.gob.pjpuebla.asistencia.core.usuario.Usuario;
import mx.gob.pjpuebla.asistencia.core.usuario.UsuarioRepository;
import mx.gob.pjpuebla.asistencia.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import mx.gob.pjpuebla.asistencia.core.area.Area;
import mx.gob.pjpuebla.asistencia.util.enums.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final HorarioRepository horarioRepository;
    private final SecurityUtil securityUtil;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void registrarEntrada(MultipartFile foto) {
        Usuario currentUser = securityUtil.getCurrentUser().orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
        LocalDate hoy = LocalDate.now();

        Optional<Asistencia> asistenciaHoy = asistenciaRepository.findByUsuarioAndFecha(currentUser, LocalDate.now());
        if (asistenciaHoy.isPresent() && asistenciaHoy.get().getHoraEntrada() != null) {
            throw new IllegalStateException("Ya existe un registro de entrada para hoy.");
        }

        boolean esRetardo = esTarde(currentUser);

        Asistencia nuevaAsistencia = new Asistencia();
        nuevaAsistencia.setUsuario(currentUser);
        nuevaAsistencia.setFecha(hoy);
        nuevaAsistencia.setHoraEntrada(LocalDateTime.now());
        nuevaAsistencia.setFotoEntrada(convertirMultipartABase64(foto)); // Conversión a Base64
        nuevaAsistencia.setEsRetardo(esRetardo);
        
        asistenciaRepository.save(nuevaAsistencia);
    }

    @Transactional
    public void registrarSalida(MultipartFile foto) {
        Usuario currentUser = securityUtil.getCurrentUser().orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
        LocalDate hoy = LocalDate.now();

        Asistencia asistenciaHoy = asistenciaRepository.findByUsuarioAndFecha(currentUser, hoy)
                .orElseThrow(() -> new IllegalStateException("Debe registrar una entrada antes de registrar una salida."));

        if (asistenciaHoy.getHoraSalida() != null) {
            throw new IllegalStateException("Ya existe un registro de salida para hoy.");
        }

        asistenciaHoy.setHoraSalida(LocalDateTime.now());
        asistenciaHoy.setFotoSalida(convertirMultipartABase64(foto)); // Conversión a Base64

        asistenciaRepository.save(asistenciaHoy);
    }

    private String convertirMultipartABase64(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return "data:" + file.getContentType() + ";base64," + base64;
        } catch (IOException e) {
            throw new RuntimeException("Error al procesar el archivo de imagen.", e);
        }
    }

    private boolean esTarde(Usuario usuario) {
        // Usamos el área principal del usuario, que nunca será nula.
        Integer areaPrincipalId = usuario.getAreaPrincipal().getId();
        
        Horario horario = horarioRepository.findBestMatchForUsuario(usuario.getId(), areaPrincipalId)
                .stream().findFirst()
                .orElse(null);

        if (horario == null) return false;

        LocalTime horaDeEntradaConTolerancia = horario.getHoraEntrada().plusMinutes(horario.getToleranciaMinutos());
        
        return LocalTime.now().isAfter(horaDeEntradaConTolerancia);
    }

    @Transactional(readOnly = true)
    public Page<AsistenciaReporteRecord> getReporteAsistencias(
        Optional<LocalDate> fechaInicio, Optional<LocalDate> fechaFin,
        Optional<Integer> usuarioId, Optional<Integer> areaId, Optional<String> key,
        Pageable pageable
    ) {
        Specification<Asistencia> spec = createAsistenciaSpecification(fechaInicio, fechaFin, usuarioId, areaId, key, Optional.of(false)); // Retardos no se filtra aquí
        return asistenciaRepository.findAll(spec, pageable).map(this::toReporteRecord);
    }

    // Método para obtener datos para reportes (sin paginar)
    @Transactional(readOnly = true)
    public List<AsistenciaReporteRecord> getReporteData(
        Optional<LocalDate> fechaInicio, Optional<LocalDate> fechaFin,
        Optional<Integer> usuarioId, Optional<Integer> areaId,
        Optional<String> key, Optional<Boolean> soloRetardos
    ) {
        Specification<Asistencia> spec = createAsistenciaSpecification(fechaInicio, fechaFin, usuarioId, areaId, key, soloRetardos);
        // Usamos findAll sin paginación, pero con ordenamiento por fecha
        List<Asistencia> asistencias = asistenciaRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "fecha"));
        return asistencias.stream().map(this::toReporteRecord).collect(Collectors.toList());
    }

    // Método privado y reutilizable para crear la consulta de filtros
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
                        criteriaBuilder.like(criteriaBuilder.lower(usuarioJoin.get("matricula")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(usuarioJoin.get("nombre")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(usuarioJoin.get("apellidoPaterno")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(usuarioJoin.get("apellidoMaterno")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(areaJoin.get("nombre")), pattern)
                    ));
                }
            });

            soloRetardos.ifPresent(retardos -> {
                if (retardos) {
                    predicates.add(criteriaBuilder.isTrue(root.get("esRetardo")));
                }
            });

            // Lógica de seguridad por rol
            if (currentUser.getRol() == Rol.SUPERADMIN) {
                areaId.ifPresent(id -> predicates.add(criteriaBuilder.equal(usuarioJoin.get("areaPrincipal").get("id"), id)));
            } else if (currentUser.getRol() == Rol.ADMIN) {
                // Un Admin SÓLO puede ver las áreas que tiene asignadas
                Set<Integer> idsDeSusAreas = currentUser.getAreasGestionadas().stream()
                                                    .map(Area::getId)
                                                    .collect(Collectors.toSet());
                idsDeSusAreas.add(currentUser.getAreaPrincipal().getId());

                if (idsDeSusAreas.isEmpty()) {
                    return criteriaBuilder.disjunction();
                }
                
                // Añade la condición de que el área del usuario debe estar en la lista de áreas permitidas.
                predicates.add(usuarioJoin.get("areaPrincipal").get("id").in(idsDeSusAreas));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional
public AsistenciaReporteRecord createManual(AsistenciaManualRecord record) {
    Usuario usuario = usuarioRepository.findById(record.usuarioId())
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    Asistencia entity = new Asistencia();
    entity.setUsuario(usuario);
    entity.setFecha(record.fecha());
    entity.setHoraEntrada(record.horaEntrada());
    entity.setHoraSalida(record.horaSalida());
    entity.setEsRetardo(record.esRetardo());

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
    entity.setEsRetardo(record.esRetardo());

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
public Map<String, Boolean> getEstadoAsistenciaDiario(String matricula) {
        Usuario usuario = usuarioRepository.findByMatricula(matricula)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con matrícula: " + matricula));

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
                entity.getEsRetardo(),
                usuario.getId(),
                usuario.getMatricula(),
                nombreCompleto.trim(),
                area.getId(),
                area.getNombre(),
                entity.getFotoEntrada(),
                entity.getFotoSalida()
        );
    }
}