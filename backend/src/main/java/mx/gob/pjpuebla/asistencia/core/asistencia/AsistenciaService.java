package mx.gob.pjpuebla.asistencia.core.asistencia;

import lombok.RequiredArgsConstructor;
import mx.gob.pjpuebla.asistencia.core.horario.Horario;
import mx.gob.pjpuebla.asistencia.core.horario.HorarioRepository;
import mx.gob.pjpuebla.asistencia.core.usuario.Usuario;
import mx.gob.pjpuebla.asistencia.core.usuario.UsuarioRepository;
import mx.gob.pjpuebla.asistencia.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mx.gob.pjpuebla.asistencia.core.area.Area;
import mx.gob.pjpuebla.asistencia.util.enums.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;

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
    public void registrarEntrada(RegistroAsistenciaRequest request) {
        Usuario currentUser = securityUtil.getCurrentUser().orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
        LocalDate hoy = LocalDate.now();

        // Verificar que no haya una entrada registrada para hoy
        Optional<Asistencia> asistenciaHoy = asistenciaRepository.findByUsuarioAndFecha(currentUser, LocalDate.now());
        if (asistenciaHoy.isPresent()) {
            // Si ya hay un registro, y ya tiene hora de entrada, lanzar excepción
            if (asistenciaHoy.get().getHoraEntrada() != null) {
                throw new IllegalStateException("Ya existe un registro de entrada para hoy.");
            }
        }
        // Verificar si llegó tarde
        boolean esRetardo = esTarde(currentUser);

        // Crear y guardar el nuevo registro de asistencia
        Asistencia nuevaAsistencia = new Asistencia();
        nuevaAsistencia.setUsuario(currentUser);
        nuevaAsistencia.setFecha(hoy);
        nuevaAsistencia.setHoraEntrada(LocalDateTime.now());
        nuevaAsistencia.setFotoEntrada(request.fotoBase64());
        nuevaAsistencia.setEsRetardo(esRetardo);
        
        asistenciaRepository.save(nuevaAsistencia);
    }

    @Transactional
    public void registrarSalida(RegistroAsistenciaRequest request) {
        Usuario currentUser = securityUtil.getCurrentUser().orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
        LocalDate hoy = LocalDate.now();

        // Buscar el registro de entrada de hoy
        Asistencia asistenciaHoy = asistenciaRepository.findByUsuarioAndFecha(currentUser, hoy)
                .orElseThrow(() -> new IllegalStateException("Debe registrar una entrada antes de registrar una salida."));

        if (asistenciaHoy.getHoraSalida() != null) {
            throw new IllegalStateException("Ya existe un registro de salida para hoy.");
        }

        // Verificar que la salida no haya sido registrada previamente
        if (asistenciaHoy.getHoraSalida() != null) {
            throw new IllegalStateException("Ya existe un registro de salida para hoy.");
        }

        // Actualizar el registro con la hora y foto de salida
        asistenciaHoy.setHoraSalida(LocalDateTime.now());
        asistenciaHoy.setFotoSalida(request.fotoBase64());

        asistenciaRepository.save(asistenciaHoy);
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
        Optional<LocalDate> fechaInicio,
        Optional<LocalDate> fechaFin,
        Optional<Integer> usuarioId,
        Optional<Integer> areaId,
        Pageable pageable
    ) {
        Usuario currentUser = securityUtil.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        Specification<Asistencia> spec = (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Aplicación de los filtros generales
            fechaInicio.ifPresent(fecha -> predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fecha"), fecha)));
            fechaFin.ifPresent(fecha -> predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fecha"), fecha)));
            usuarioId.ifPresent(id -> predicates.add(criteriaBuilder.equal(root.get("usuario").get("id"), id)));


            // Lógica de seguridad por rol
            if (currentUser.getRol() == Rol.SUPERADMIN) {
                // El Superadmin puede filtrar por cualquier área si lo desea
                areaId.ifPresent(id -> predicates.add(criteriaBuilder.equal(root.get("usuario").get("areaPrincipal").get("id"), id)));
            } else if (currentUser.getRol() == Rol.ADMIN) {
                // El Admin SÓLO puede ver las áreas que tiene asignadas
                Set<Integer> idsDeSusAreas = currentUser.getAreasGestionadas().stream()
                                                    .map(Area::getId)
                                                    .collect(Collectors.toSet());
                idsDeSusAreas.add(currentUser.getAreaPrincipal().getId());

                if (idsDeSusAreas.isEmpty()) {
                    return criteriaBuilder.disjunction();
                }
                predicates.add(root.get("usuario").get("areaPrincipal").get("id").in(idsDeSusAreas));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return asistenciaRepository.findAll(spec, pageable).map(this::toReporteRecord);
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

    // --- MÉTODO DE MAPEO ---
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