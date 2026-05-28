package mx.gob.sedif.asistencia.util;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import mx.gob.sedif.asistencia.security.SecurityUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditableListener {

    private static SecurityUtil securityUtil;

    @Autowired
    public void init(SecurityUtil securityUtil) {
        AuditableListener.securityUtil = securityUtil;
    }

    @PrePersist
    public void prePersist(Object o) {
        if (o instanceof Auditable) {
            Auditable auditable = (Auditable) o;
            Audit audit = new Audit();
            audit.setFechaAlta(LocalDateTime.now());

            // Cambiamos de getMatricula a getNumeroControl
            String numeroControl = securityUtil.getCurrentUser()
                    .map(user -> user.getNumeroControl())
                    .orElse("SISTEMA");
            audit.setUsuarioAlta(numeroControl);
            auditable.setAudit(audit);
        }
    }

    @PreUpdate
    public void preUpdate(Object o) {
        if (o instanceof Auditable) {
            Auditable auditable = (Auditable) o;
            Audit audit = auditable.getAudit();
            
            // Verificamos si el audit no es nulo por si es una entidad vieja que no tenía el bloque
            if (audit == null) {
                audit = new Audit();
                audit.setFechaAlta(LocalDateTime.now()); // Fallback en caso de que viniera vacío
            }
            
            audit.setFechaEdita(LocalDateTime.now());

            // Cambiamos de getMatricula a getNumeroControl
            String numeroControl = securityUtil.getCurrentUser()
                    .map(user -> user.getNumeroControl())
                    .orElse("SISTEMA");
            audit.setUsuarioEdita(numeroControl);
            auditable.setAudit(audit);
        }
    }
}