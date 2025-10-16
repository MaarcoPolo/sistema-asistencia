package mx.gob.pjpuebla.asistencia.core.area;

import lombok.RequiredArgsConstructor;
import mx.gob.pjpuebla.asistencia.core.usuario.Usuario; 
import mx.gob.pjpuebla.asistencia.core.usuario.UsuarioRepository;
import mx.gob.pjpuebla.asistencia.security.SecurityUtil; 
import mx.gob.pjpuebla.asistencia.util.enums.Estado;
import mx.gob.pjpuebla.asistencia.util.enums.Rol; 

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set; 
import java.util.LinkedList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaService {
    private final AreaRepository areaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityUtil securityUtil; 

    @Transactional(readOnly = true)
    public Set<Integer> obtenerIdsDeAreasGestionadasPorAdmin(Usuario admin) {
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return new HashSet<>();
        }
        Usuario managedAdmin = usuarioRepository.findById(admin.getId())
            .orElseThrow(() -> new RuntimeException("No se pudo recargar el usuario Admin para verificar permisos."));

        Set<Area> areasBase = new HashSet<>(managedAdmin.getAreasGestionadas());
        areasBase.add(managedAdmin.getAreaPrincipal());

        return obtenerTodosLosIdsDeAreasDescendientes(areasBase);
    }

    private Set<Integer> obtenerTodosLosIdsDeAreasDescendientes(Set<Area> areasIniciales) {
        Set<Area> areasEncontradas = new HashSet<>(areasIniciales);
        Queue<Area> areasAProcesar = new LinkedList<>(areasIniciales);

        while (!areasAProcesar.isEmpty()) {
            Set<Area> padresActuales = new HashSet<>();
            while(!areasAProcesar.isEmpty()){
                padresActuales.add(areasAProcesar.poll());
            }
            
            List<Area> hijos = areaRepository.findByAreaPadreInAndEstatus(padresActuales, Estado.ACTIVE);

            for (Area hijo : hijos) {
                if (areasEncontradas.add(hijo)) {
                    areasAProcesar.add(hijo);
                }
            }
        }
        return areasEncontradas.stream().map(Area::getId).collect(Collectors.toSet());
    }


    @Transactional(readOnly = true)
    public Page<AreaRecord> getAll(String key, Pageable pageable) {
        // Ahora aplica la lógica de seguridad por rol
        Usuario currentUser = securityUtil.getCurrentUser()
            .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        if (currentUser.getRol() == Rol.ADMIN) {
            Set<Integer> idsPermitidos = this.obtenerIdsDeAreasGestionadasPorAdmin(currentUser);
            if (idsPermitidos.isEmpty()) return Page.empty();
            
            Page<Area> areaPage = areaRepository.findByNombreAndIds(key, idsPermitidos, pageable);
            return areaPage.map(this::toRecord);
        }
        // para superadmin
        Page<Area> areaPage = areaRepository.findByNombreContainingIgnoreCaseAndEstatusNot(key, Estado.DELETED, pageable);
        return areaPage.map(this::toRecord);
    }

    @Transactional(readOnly = true)
    public List<AreaRecord> findAllForSelect() {
       // las áreas permitidas para el rol
        Usuario currentUser = securityUtil.getCurrentUser()
            .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

        List<Area> areas = new ArrayList<>();

        if (currentUser.getRol() == Rol.SUPERADMIN) {
            areas = areaRepository.findByEstatus(Estado.ACTIVE);
        } else if (currentUser.getRol() == Rol.ADMIN) {
            // Un admin solo puede ver sus áreas gestionadas y su área principal
            Set<Integer> idsPermitidos = this.obtenerIdsDeAreasGestionadasPorAdmin(currentUser);
            if (!idsPermitidos.isEmpty()) {
                areas = areaRepository.findAllById(idsPermitidos);
            }
        }
        
        // Filtramos para evitar duplicados y asegurar que solo sean activas
        return areas.stream()
                .filter(area -> area.getEstatus() == Estado.ACTIVE)
                .distinct()
                .map(area -> new AreaRecord(area.getId(), null, area.getNombre(), null, null, null, null, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AreaRecord findById(Integer id) {
        return areaRepository.findById(id)
                .map(this::toRecord)
                .orElseThrow(() -> new RuntimeException("Área no encontrada con ID: " + id));
    }

    @Transactional
    public AreaRecord create(AreaRecord record) {
        Area entity = new Area();
        this.mapToEntity(record, entity);
        entity = areaRepository.save(entity);
        return toRecord(entity);
    }

    @Transactional
    public AreaRecord save(AreaRecord record) {
        Area entity = areaRepository.findById(record.id())
                .orElseThrow(() -> new RuntimeException("Área no encontrada para actualizar con ID: " + record.id()));
        this.mapToEntity(record, entity);
        entity = areaRepository.save(entity);
        return toRecord(entity);
    }

    @Transactional
    public void deleteById(Integer id) {
        Area entity = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Área no encontrada para eliminar con ID: " + id));
        entity.setEstatus(Estado.DELETED);
        areaRepository.save(entity);
    }

    // --- Mapeo ---
    private AreaRecord toRecord(Area entity) {
        return new AreaRecord(
                entity.getId(),
                entity.getClave(),
                entity.getNombre(),
                entity.getEstatus(),
                entity.getEstatus().getEtiqueta(),
                entity.getAreaPadre() != null ? entity.getAreaPadre().getId() : null,
                entity.getAreaPadre() != null ? entity.getAreaPadre().getNombre() : null,
                entity.getIpPermitida()
        );
    }

    private void mapToEntity(AreaRecord record, Area entity) {
        entity.setClave(record.clave());
        entity.setNombre(record.nombre());
        entity.setEstatus(record.estatus());
        entity.setIpPermitida(record.ipPermitida());
        if (record.idAreaPadre() != null) {
            Area areaPadre = areaRepository.findById(record.idAreaPadre())
                    .orElseThrow(() -> new RuntimeException("Área padre no encontrada"));
            entity.setAreaPadre(areaPadre);
        } else {
            entity.setAreaPadre(null);
        }
    }
}