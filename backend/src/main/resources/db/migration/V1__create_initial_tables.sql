-- V1__create_initial_tables.sql

-- Tabla para las Áreas o Departamentos
CREATE TABLE asistencia.tbl_area (
    pn_id SERIAL PRIMARY KEY,
    s_clave VARCHAR(20) NOT NULL UNIQUE,
    s_nombre VARCHAR(100) NOT NULL,
    n_estatus INT NOT NULL,
    fn_area_padre INT, 
    -- Columnas de Auditoría
    t_fecha_alta TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    s_usuario_alta VARCHAR(50) NOT NULL,
    t_fecha_edita TIMESTAMP WITHOUT TIME ZONE,
    s_usuario_edita VARCHAR(50),

    CONSTRAINT fk_area_padre FOREIGN KEY (fn_area_padre) REFERENCES asistencia.tbl_area(pn_id)
);

-- Tabla para los Usuarios del sistema
CREATE TABLE asistencia.tbl_usuario (
    pn_id SERIAL PRIMARY KEY,
    s_matricula VARCHAR(50) NOT NULL UNIQUE,
    s_nombre VARCHAR(100) NOT NULL,
    s_apellido_paterno VARCHAR(100) NOT NULL,
    s_apellido_materno VARCHAR(100),
    -- Para administradores. La matrícula puede ser su nombre de usuario.
    s_password VARCHAR(255),
    -- 0: SUPERADMIN, 1: ADMIN, 2: USER
    n_rol INT NOT NULL,
    n_estatus INT NOT NULL,
        fn_area_id INT NOT NULL, -- <-- Es el área principal del usuario.
    -- Columnas de Auditoría
    t_fecha_alta TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    s_usuario_alta VARCHAR(50),
    t_fecha_edita TIMESTAMP WITHOUT TIME ZONE,
    s_usuario_edita VARCHAR(50),
    CONSTRAINT fk_usuario_area FOREIGN KEY (fn_area_id) REFERENCES asistencia.tbl_area(pn_id)
);

-- Tabla para definir los horarios
CREATE TABLE asistencia.tbl_horario (
    pn_id SERIAL PRIMARY KEY,
    s_nombre VARCHAR(100) NOT NULL, -- Ej: "Horario Matutino Oficinas"
    t_hora_entrada TIME NOT NULL,
    t_hora_salida TIME NOT NULL,
    n_tolerancia_minutos INT NOT NULL DEFAULT 10,
    fn_usuario_id INT UNIQUE, -- Horario específico para un usuario
    fn_area_id INT,           -- Horario general para un área
    -- Columnas de Auditoría
    t_fecha_alta TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    s_usuario_alta VARCHAR(50),
    t_fecha_edita TIMESTAMP WITHOUT TIME ZONE,
    s_usuario_edita VARCHAR(50),
    CONSTRAINT fk_horario_usuario FOREIGN KEY (fn_usuario_id) REFERENCES asistencia.tbl_usuario(pn_id),
    CONSTRAINT fk_horario_area FOREIGN KEY (fn_area_id) REFERENCES asistencia.tbl_area(pn_id)
);

-- Tabla principal para registrar las asistencias
CREATE TABLE asistencia.tbl_asistencia (
    pn_id BIGSERIAL PRIMARY KEY,
    fn_usuario_id INT NOT NULL,
    d_fecha DATE NOT NULL,
    t_hora_entrada TIMESTAMP WITHOUT TIME ZONE,
    t_hora_salida TIMESTAMP WITHOUT TIME ZONE,
    -- La foto se guardará como un texto muy largo (Base64)
    s_foto_entrada TEXT,
    s_foto_salida TEXT,
    -- Para saber si la entrada fue tardía
    b_es_retardo BOOLEAN NOT NULL DEFAULT FALSE,
    -- Restricción para asegurar una sola entrada por usuario por día
    CONSTRAINT uq_usuario_fecha UNIQUE (fn_usuario_id, d_fecha),
    CONSTRAINT fk_asistencia_usuario FOREIGN KEY (fn_usuario_id) REFERENCES asistencia.tbl_usuario(pn_id)
);

-- Esta nueva tabla crea las conexiones entre admins y áreas
CREATE TABLE asistencia.tbl_admin_area (
    fn_usuario_id INT NOT NULL,
    fn_area_id INT NOT NULL,
    PRIMARY KEY (fn_usuario_id, fn_area_id),
    CONSTRAINT fk_adminarea_usuario FOREIGN KEY (fn_usuario_id) REFERENCES asistencia.tbl_usuario(pn_id),
    CONSTRAINT fk_adminarea_area FOREIGN KEY (fn_area_id) REFERENCES asistencia.tbl_area(pn_id)
);