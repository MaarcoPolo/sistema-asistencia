package mx.gob.pjpuebla.asistencia.core.usuario;

import lombok.RequiredArgsConstructor;
import mx.gob.pjpuebla.asistencia.core.area.Area;
import mx.gob.pjpuebla.asistencia.core.area.AreaRepository;
import mx.gob.pjpuebla.asistencia.security.SecurityUtil;
import mx.gob.pjpuebla.asistencia.util.enums.Estado;
import mx.gob.pjpuebla.asistencia.util.enums.Rol;

import java.util.HashSet;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public Page<UsuarioRecord> getAll(String key, Pageable pageable) {
        Usuario currentUser = securityUtil.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        Page<Usuario> usuariosPage;

        if (currentUser.getRol() == Rol.SUPERADMIN) {
            // El Superadmin busca en todos los usuarios sin filtro de área.
            usuariosPage = usuarioRepository.findByNombreCompletoContaining(key, pageable);
        } 
        else if (currentUser.getRol() == Rol.ADMIN) {

            // Creamos un conjunto para guardar los IDs de las áreas permitidas.
            Set<Integer> idsDeSusAreas = currentUser.getAreasGestionadas().stream()
                                                .map(Area::getId)
                                                .collect(Collectors.toSet());

            // Añadimos también su área principal a la lista.
            idsDeSusAreas.add(currentUser.getAreaPrincipal().getId());

            if (idsDeSusAreas.isEmpty()) {
                // Si por alguna razón un admin no tiene áreas, no puede ver a nadie.
                return Page.empty(pageable);
            }

            // Llamamos al nuevo método del repositorio con la lista de IDs.
            usuariosPage = usuarioRepository.findByNombreCompletoInAreaIds(key, idsDeSusAreas, pageable);

        } 
        else {
            // Un usuario normal no tiene permisos.
            return Page.empty(pageable);
        }

        return usuariosPage.map(this::toRecord);
    }

    @Transactional(readOnly = true)
    public UsuarioRecord findById(Integer id) {
        Usuario currentUser = securityUtil.getCurrentUser().orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
        Usuario usuarioEncontrado = usuarioRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Un admin solo puede ver usuarios de su propia área
        if (currentUser.getRol() == Rol.ADMIN && !usuarioEncontrado.getAreaPrincipal().getId().equals(currentUser.getAreaPrincipal().getId())) {
            throw new SecurityException("No tiene permisos para ver este usuario.");
        }
        
        return toRecord(usuarioEncontrado);
    }

    @Transactional
    public UsuarioRecord create(UsuarioRecord record) {
        Usuario currentUser = securityUtil.getCurrentUser().orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        // Un admin solo puede crear usuarios en su propia área y no puede crear otros admins o superadmins
        if (currentUser.getRol() == Rol.ADMIN) {
            if (!record.idAreaPrincipal().equals(currentUser.getAreaPrincipal().getId())) {
                throw new SecurityException("No puede crear usuarios fuera de su área.");
            }
            if (record.rol() == Rol.SUPERADMIN || record.rol() == Rol.ADMIN) {
                throw new SecurityException("No tiene permisos para crear usuarios con este rol.");
            }
        }
        
        if (usuarioRepository.findByMatricula(record.matricula()).isPresent()) {
            throw new RuntimeException("La matrícula ya está en uso.");
        }
        
        Usuario entity = new Usuario();
        this.mapToEntity(record, entity);

        // Solo validar y encriptar contraseña si el rol lo requiere
        if (record.rol() == Rol.ADMIN || record.rol() == Rol.SUPERADMIN) {
            if (record.password() == null || record.password().isBlank()) {
                throw new RuntimeException("La contraseña es obligatoria para crear un usuario Administrador.");
            }
            entity.setPassword(passwordEncoder.encode(record.password()));
        } else {
            // Para el rol USER, la contraseña es nula.
            entity.setPassword(null);
        }

        entity = usuarioRepository.save(entity);
        return toRecord(entity);
    }

    @Transactional
    public UsuarioRecord save(UsuarioRecord record) {
        Usuario currentUser = securityUtil.getCurrentUser().orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
        Usuario entityToUpdate = usuarioRepository.findById(record.id()).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (currentUser.getRol() == Rol.ADMIN) {
            // Un ADMIN no puede editar a un SUPERADMIN.
            if (entityToUpdate.getRol() == Rol.SUPERADMIN) {
                throw new SecurityException("No tiene permisos para editar a un Superadministrador.");
            }
            
            // Un ADMIN no puede editar a otro ADMIN
            if (entityToUpdate.getRol() == Rol.ADMIN && !currentUser.getId().equals(entityToUpdate.getId())) {
                throw new SecurityException("No tiene permisos para editar a otro Administrador.");
            }

            // Un ADMIN solo puede editar usuarios de sus áreas gestionadas.
            Set<Integer> idsDeSusAreas = currentUser.getAreasGestionadas().stream()
                                                .map(Area::getId)
                                                .collect(Collectors.toSet());
            idsDeSusAreas.add(currentUser.getAreaPrincipal().getId());

            if (!idsDeSusAreas.contains(entityToUpdate.getAreaPrincipal().getId())) {
                throw new SecurityException("No tiene permisos para editar usuarios de esta área.");
            }
        }
        
        this.mapToEntity(record, entityToUpdate);

        if (record.password() != null && !record.password().isBlank()) {
            entityToUpdate.setPassword(passwordEncoder.encode(record.password()));
        }

        entityToUpdate = usuarioRepository.save(entityToUpdate);
        return toRecord(entityToUpdate);
    }

    @Transactional
    public void deleteById(Integer id) {
        Usuario currentUser = securityUtil.getCurrentUser().orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
        Usuario entity = usuarioRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado para eliminar"));
        
        // Un admin solo puede eliminar usuarios de su propia área
        if (currentUser.getRol() == Rol.ADMIN && !entity.getAreaPrincipal().getId().equals(currentUser.getAreaPrincipal().getId())) {
            throw new SecurityException("No tiene permisos para eliminar este usuario.");
        }

        entity.setEstatus(Estado.DELETED);
        usuarioRepository.save(entity);
    }

    // --- Métodos de Mapeo ---
    private UsuarioRecord toRecord(Usuario entity) {
        String nombreCompleto = entity.getNombre() + " " + entity.getApellidoPaterno() + 
                                (entity.getApellidoMaterno() != null ? " " + entity.getApellidoMaterno() : "");
                                
        Set<Integer> idsAreasGestionadas = entity.getAreasGestionadas() != null ?
        entity.getAreasGestionadas().stream().map(Area::getId).collect(Collectors.toSet()) :
        new HashSet<>();

        return new UsuarioRecord(
                entity.getId(),
                entity.getMatricula(),
                entity.getNombre(),
                entity.getApellidoPaterno(),
                entity.getApellidoMaterno(),
                null,
                entity.getRol(),
                entity.getEstatus(),
                entity.getAreaPrincipal().getId(),
                idsAreasGestionadas,
                nombreCompleto.trim(),
                entity.getRol().getEtiqueta(),
                entity.getEstatus().getEtiqueta(),
                entity.getAreaPrincipal().getNombre()
        );
    }

    private void mapToEntity(UsuarioRecord record, Usuario entity) {
        Area areaPrincipal = areaRepository.findById(record.idAreaPrincipal()).orElseThrow(() -> new RuntimeException("Área principal no encontrada"));
        entity.setMatricula(record.matricula());
        entity.setNombre(record.nombre());
        entity.setApellidoPaterno(record.apellidoPaterno());
        entity.setApellidoMaterno(record.apellidoMaterno());
        entity.setRol(record.rol());
        entity.setEstatus(record.estatus());
        entity.setAreaPrincipal(areaPrincipal);

        // Asignar las áreas gestionadas solo si el rol es ADMIN
        if (record.rol() == Rol.ADMIN && record.idsAreasGestionadas() != null) {
            Set<Area> areasGestionadas = new HashSet<>(areaRepository.findAllById(record.idsAreasGestionadas()));
            entity.setAreasGestionadas(areasGestionadas);
        } else {
            entity.setAreasGestionadas(new HashSet<>()); // Limpiar si no es admin
        }
    }
}