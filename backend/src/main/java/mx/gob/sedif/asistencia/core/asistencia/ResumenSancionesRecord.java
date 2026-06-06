package mx.gob.sedif.asistencia.core.asistencia;

import java.time.LocalDate;
import java.util.List;

public record ResumenSancionesRecord(
    String numeroControl,
    String nombreCompleto,
    String area,
    long totalRetardos,
    double diasDescuentoRetardos,
    List<LocalDate> fechasRetardos,
    long totalFaltasYOmisiones,
    double diasDescuentoFaltas,
    List<LocalDate> fechasFaltasYOmisiones,
    double totalDiasDescontar
) {}