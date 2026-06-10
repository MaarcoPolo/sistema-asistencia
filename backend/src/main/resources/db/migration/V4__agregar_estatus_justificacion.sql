-- Agrega la nueva columna de estatus a la tabla de justificaciones.
-- Como la columna en Java es 'nullable = false', le damos el valor por defecto 'PENDIENTE' 
-- para que no falle con los registros que ya existen en la base de datos.

ALTER TABLE asistencia.tbl_asistencia_justificacion
ADD COLUMN s_estatus VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE';