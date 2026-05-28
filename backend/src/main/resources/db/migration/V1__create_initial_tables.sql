-- V1__create_initial_tables.sql
-- (Estructura rediseñada: Número de control, turnos 1y1, cruce de medianoche, incidencias y justificaciones)

-- 1. Tabla para las Áreas o Departamentos
CREATE TABLE asistencia.tbl_area (
    pn_id SERIAL PRIMARY KEY,
    s_clave VARCHAR(20) NOT NULL UNIQUE,
    s_nombre VARCHAR(100) NOT NULL,
    n_estatus INT NOT NULL,
    fn_area_padre INT,
    s_ip_permitida VARCHAR(45) NULL, 
    
    t_fecha_alta TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    s_usuario_alta VARCHAR(50) NOT NULL,
    t_fecha_edita TIMESTAMP WITHOUT TIME ZONE,
    s_usuario_edita VARCHAR(50),

    CONSTRAINT fk_area_padre FOREIGN KEY (fn_area_padre) REFERENCES asistencia.tbl_area(pn_id)
);

-- 2. Tabla para los Usuarios del sistema
CREATE TABLE asistencia.tbl_usuario (
    pn_id SERIAL PRIMARY KEY,
    s_numero_control VARCHAR(50) NOT NULL UNIQUE, -- Reemplaza a la antigua matrícula
    s_nombre VARCHAR(100) NOT NULL,
    s_apellido_paterno VARCHAR(100) NOT NULL,
    s_apellido_materno VARCHAR(100),
    s_password VARCHAR(255),
    n_rol INT NOT NULL, 
    n_estatus INT NOT NULL,
    fn_area_id INT NOT NULL, 
    
    t_fecha_alta TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    s_usuario_alta VARCHAR(50) NOT NULL,
    t_fecha_edita TIMESTAMP WITHOUT TIME ZONE,
    s_usuario_edita VARCHAR(50),

    CONSTRAINT fk_usuario_area FOREIGN KEY (fn_area_id) REFERENCES asistencia.tbl_area(pn_id)
);

-- 3. Tabla de conexiones entre admins y áreas
CREATE TABLE asistencia.tbl_admin_area (
    fn_usuario_id INT NOT NULL,
    fn_area_id INT NOT NULL,
    PRIMARY KEY (fn_usuario_id, fn_area_id),
    CONSTRAINT fk_admin_area_usuario FOREIGN KEY (fn_usuario_id) REFERENCES asistencia.tbl_usuario(pn_id),
    CONSTRAINT fk_admin_area_area FOREIGN KEY (fn_area_id) REFERENCES asistencia.tbl_area(pn_id)
);

-- ==========================================
-- EL NUEVO MOTOR DE HORARIOS
-- ==========================================

-- 4. Catálogo Maestro de Horarios
CREATE TABLE asistencia.tbl_horario (
    pn_id SERIAL PRIMARY KEY,
    s_nombre VARCHAR(150) NOT NULL, 
    b_cruce_medianoche BOOLEAN NOT NULL DEFAULT FALSE, 
    n_tipo_ciclo INT NOT NULL DEFAULT 1, 
    
    t_fecha_alta TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    s_usuario_alta VARCHAR(50) NOT NULL,
    t_fecha_edita TIMESTAMP WITHOUT TIME ZONE,
    s_usuario_edita VARCHAR(50)
);

-- 5. Detalle del Horario (Soporta múltiples horas por día)
CREATE TABLE asistencia.tbl_horario_detalle (
    pn_id SERIAL PRIMARY KEY,
    fn_horario_id INT NOT NULL,
    n_dia INT NOT NULL, 
    t_hora_entrada TIME WITHOUT TIME ZONE NOT NULL,
    t_hora_salida TIME WITHOUT TIME ZONE NOT NULL,
    n_tolerancia_minutos INT NOT NULL DEFAULT 15,
    
    CONSTRAINT fk_detalle_horario FOREIGN KEY (fn_horario_id) REFERENCES asistencia.tbl_horario(pn_id)
);

-- 6. Asignación del Horario al Usuario (Con Día Pivote)
CREATE TABLE asistencia.tbl_horario_usuario (
    fn_usuario_id INT NOT NULL,
    fn_horario_id INT NOT NULL,
    fn_area_id INT NOT NULL,
    d_fecha_inicio_ciclo DATE NULL, 
    
    t_fecha_alta TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    s_usuario_alta VARCHAR(50),
    t_fecha_edita TIMESTAMP WITHOUT TIME ZONE,
    s_usuario_edita VARCHAR(50),
    
    CONSTRAINT pk_horario_usuario PRIMARY KEY (fn_usuario_id, fn_horario_id),
    CONSTRAINT fk_hu_usuario FOREIGN KEY (fn_usuario_id) REFERENCES asistencia.tbl_usuario(pn_id),
    CONSTRAINT fk_hu_horario FOREIGN KEY (fn_horario_id) REFERENCES asistencia.tbl_horario(pn_id),
    CONSTRAINT fk_hu_area FOREIGN KEY (fn_area_id) REFERENCES asistencia.tbl_area(pn_id)
);

-- 7. Excepciones de Horario (Para el módulo de RH de cambios manuales)
CREATE TABLE asistencia.tbl_excepcion_horario (
    pn_id SERIAL PRIMARY KEY,
    fn_usuario_id INT NOT NULL,
    d_fecha_especifica DATE NOT NULL,
    b_labora BOOLEAN NOT NULL, 
    s_motivo VARCHAR(255),
    
    t_fecha_alta TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    s_usuario_alta VARCHAR(50) NOT NULL,
    
    CONSTRAINT fk_excepcion_usuario FOREIGN KEY (fn_usuario_id) REFERENCES asistencia.tbl_usuario(pn_id)
);

-- ==========================================
-- REGISTRO DE ASISTENCIAS Y JUSTIFICACIONES
-- ==========================================

-- 8. Tabla principal de Asistencias (Optimizada para biometría)
CREATE TABLE asistencia.tbl_asistencia (
    pn_id BIGSERIAL PRIMARY KEY,
    fn_usuario_id INT NOT NULL,
    d_fecha DATE NOT NULL,
    t_hora_entrada TIMESTAMP WITHOUT TIME ZONE,
    t_hora_salida TIMESTAMP WITHOUT TIME ZONE,
    
    -- Opcionales: Para usar la webcam ahora, y dejarlas vacías cuando llegue el biométrico
    s_foto_entrada TEXT,
    s_foto_salida TEXT,
    s_ip_registro VARCHAR(45) NULL,
    
    -- 0=OK, 1=Retardo, 2=Falta Total, 3=Omisión Entrada, 4=Omisión Salida
    n_estatus_incidencia INT NOT NULL DEFAULT 0, 
    
    CONSTRAINT uq_usuario_fecha UNIQUE (fn_usuario_id, d_fecha),
    CONSTRAINT fk_asistencia_usuario FOREIGN KEY (fn_usuario_id) REFERENCES asistencia.tbl_usuario(pn_id)
);

-- 9. Catálogo de Justificaciones
CREATE TABLE asistencia.tbl_catalogo_justificacion (
    pn_id SERIAL PRIMARY KEY,
    s_clave VARCHAR(10) NOT NULL UNIQUE,
    s_nombre VARCHAR(100) NOT NULL,
    b_requiere_observacion BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO asistencia.tbl_catalogo_justificacion (s_clave, s_nombre, b_requiere_observacion) VALUES 
('FV', 'Vacaciones', FALSE),
('FI', 'Incapacidad', FALSE),
('FJ', 'Comisiones', FALSE),
('FDE', 'Dia económico', FALSE),
('FC', 'Capacitaciones', FALSE),
('FSA', 'Salida Anticipada', FALSE),
('OEJ', 'Comisión Entrada', FALSE),
('OSJ', 'Comisión Salida', FALSE),
('OTROS', 'Otros', TRUE);

-- 10. Relación de Justificaciones aplicadas (Soporta archivos PDF)
CREATE TABLE asistencia.tbl_asistencia_justificacion (
    pn_id BIGSERIAL PRIMARY KEY,
    fn_asistencia_id BIGINT NOT NULL UNIQUE, 
    fn_justificacion_id INT NOT NULL,
    s_observacion TEXT,
    s_ruta_pdf VARCHAR(500), 
    
    t_fecha_registro TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    s_usuario_registro VARCHAR(50) NOT NULL,
    
    CONSTRAINT fk_aj_asistencia FOREIGN KEY (fn_asistencia_id) REFERENCES asistencia.tbl_asistencia(pn_id),
    CONSTRAINT fk_aj_catalogo FOREIGN KEY (fn_justificacion_id) REFERENCES asistencia.tbl_catalogo_justificacion(pn_id)
);