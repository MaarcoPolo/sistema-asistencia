package mx.gob.sedif.asistencia.util;

public interface Auditable {
    Audit getAudit();
    void setAudit(Audit audit);
}
