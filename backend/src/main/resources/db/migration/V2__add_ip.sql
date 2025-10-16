-- Agrega la columna para guardar la IP en cada registro de asistencia
ALTER TABLE asistencia.tbl_asistencia
ADD COLUMN s_ip_registro VARCHAR(45) NULL;

-- Agrega la columna para definir una IP permitida por área
ALTER TABLE asistencia.tbl_area
ADD COLUMN s_ip_permitida VARCHAR(45) NULL;