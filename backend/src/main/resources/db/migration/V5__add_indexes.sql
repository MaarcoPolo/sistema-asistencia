-- ────────────────────────────────────────────────────────────────────────────
-- V5: Índices secundarios para rendimiento (PERF-001)
--
-- PostgreSQL NO crea índices automáticamente sobre las columnas de clave foránea
-- (solo sobre PK y UNIQUE). Sin estos índices, los reportes y el job nocturno
-- hacen sequential scans que crecen linealmente con el volumen de datos.
--
-- Se usa IF NOT EXISTS para que la migración sea idempotente y segura de re-aplicar.
-- ────────────────────────────────────────────────────────────────────────────

-- tbl_asistencia: tabla de mayor crecimiento (≈ empleados × días laborables)
-- fn_usuario_id ya forma parte del UNIQUE (fn_usuario_id, d_fecha), pero un índice
-- dedicado acelera los JOIN y los filtros por usuario en los reportes.
CREATE INDEX IF NOT EXISTS idx_asistencia_usuario
    ON asistencia.tbl_asistencia (fn_usuario_id);
CREATE INDEX IF NOT EXISTS idx_asistencia_fecha
    ON asistencia.tbl_asistencia (d_fecha);
CREATE INDEX IF NOT EXISTS idx_asistencia_estatus
    ON asistencia.tbl_asistencia (n_estatus_incidencia);

-- tbl_usuario: filtros por área (admin) y por estatus (excluir DELETED)
CREATE INDEX IF NOT EXISTS idx_usuario_area
    ON asistencia.tbl_usuario (fn_area_id);
CREATE INDEX IF NOT EXISTS idx_usuario_estatus
    ON asistencia.tbl_usuario (n_estatus);

-- tbl_horario_usuario: consultado por usuario en el job nocturno y en el listado
CREATE INDEX IF NOT EXISTS idx_hu_usuario
    ON asistencia.tbl_horario_usuario (fn_usuario_id);

-- tbl_excepcion_horario: consultado por (usuario, fecha) en cálculo de turnos
CREATE INDEX IF NOT EXISTS idx_excepcion_usr_fecha
    ON asistencia.tbl_excepcion_horario (fn_usuario_id, d_fecha_especifica);

-- tbl_admin_area: resolución de áreas gestionadas por cada admin
CREATE INDEX IF NOT EXISTS idx_admin_area_usuario
    ON asistencia.tbl_admin_area (fn_usuario_id);

-- tbl_area: recorrido del árbol de áreas descendientes (BFS por area padre)
CREATE INDEX IF NOT EXISTS idx_area_padre
    ON asistencia.tbl_area (fn_area_padre);

-- tbl_horario_detalle: carga de los detalles de cada horario
CREATE INDEX IF NOT EXISTS idx_hdetalle_horario
    ON asistencia.tbl_horario_detalle (fn_horario_id);
