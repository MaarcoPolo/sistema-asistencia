package mx.gob.pjpuebla.asistencia.util;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import mx.gob.pjpuebla.asistencia.security.SecurityUtil;

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

            String matricula = securityUtil.getCurrentUser()
                    .map(user -> user.getMatricula())
                    .orElse("SISTEMA");
            audit.setUsuarioAlta(matricula);
            auditable.setAudit(audit);
        }
    }

    @PreUpdate
    public void preUpdate(Object o) {
        if (o instanceof Auditable) {
            Auditable auditable = (Auditable) o;
            Audit audit = auditable.getAudit();
            audit.setFechaEdita(LocalDateTime.now());

            String matricula = securityUtil.getCurrentUser()
                    .map(user -> user.getMatricula())
                    .orElse("SISTEMA");
            audit.setUsuarioEdita(matricula);
            auditable.setAudit(audit);
        }
    }
}