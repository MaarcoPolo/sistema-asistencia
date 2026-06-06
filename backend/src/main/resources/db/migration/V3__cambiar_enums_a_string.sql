-- =============================================================================
-- V3: Migrar columnas de enum ordinal (INT) a enum string (VARCHAR)
--
-- CONTEXTO: Las columnas n_rol y n_estatus de tbl_usuario almacenaban los
-- valores como enteros ordinales (0,1,2). Esto es frágil: si se reordena el
-- enum Java los datos existentes quedan corrompidos silenciosamente.
-- Esta migración convierte los enteros a los nombres de string del enum.
--
-- Mapeos:
--   n_rol:    0 → SUPERADMIN, 1 → ADMIN, 2 → USER
--   n_estatus: 0 → ACTIVE,     1 → INACTIVE, 2 → DELETED
-- =============================================================================

-- ── Columna n_rol ─────────────────────────────────────────────────────────────

-- 1. Agregar columna temporal con el valor string
ALTER TABLE asistencia.tbl_usuario ADD COLUMN s_rol_tmp VARCHAR(20);

-- 2. Poblar la columna temporal según el ordinal existente
UPDATE asistencia.tbl_usuario
SET s_rol_tmp = CASE n_rol
    WHEN 0 THEN 'SUPERADMIN'
    WHEN 1 THEN 'ADMIN'
    WHEN 2 THEN 'USER'
    ELSE 'USER'
END;

-- 3. Asegurar que no quede ningún NULL
ALTER TABLE asistencia.tbl_usuario ALTER COLUMN s_rol_tmp SET NOT NULL;

-- 4. Eliminar la columna original (integer)
ALTER TABLE asistencia.tbl_usuario DROP COLUMN n_rol;

-- 5. Renombrar la temporal al nombre definitivo
ALTER TABLE asistencia.tbl_usuario RENAME COLUMN s_rol_tmp TO n_rol;

-- ── Columna n_estatus ─────────────────────────────────────────────────────────

ALTER TABLE asistencia.tbl_usuario ADD COLUMN s_estatus_tmp VARCHAR(20);

UPDATE asistencia.tbl_usuario
SET s_estatus_tmp = CASE n_estatus
    WHEN 0 THEN 'ACTIVE'
    WHEN 1 THEN 'INACTIVE'
    WHEN 2 THEN 'DELETED'
    ELSE 'ACTIVE'
END;

ALTER TABLE asistencia.tbl_usuario ALTER COLUMN s_estatus_tmp SET NOT NULL;

ALTER TABLE asistencia.tbl_usuario DROP COLUMN n_estatus;

ALTER TABLE asistencia.tbl_usuario RENAME COLUMN s_estatus_tmp TO n_estatus;
