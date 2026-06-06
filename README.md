# Sistema de Control de Asistencia — SEDIF

Sistema web para el registro y control de asistencia del personal de la Secretaría de Desarrollo e Inclusión Familiar (SEDIF). Los empleados registran entrada y salida desde un kiosco con cámara; los administradores consultan reportes, gestionan justificaciones y exportan en Excel/PDF.

---

## Tabla de contenidos

- [Arquitectura](#arquitectura)
- [Requisitos previos](#requisitos-previos)
- [Inicio rápido con Docker](#inicio-rápido-con-docker)
- [Desarrollo local](#desarrollo-local)
- [Variables de entorno](#variables-de-entorno)
- [Estructura del proyecto](#estructura-del-proyecto)
- [API REST](#api-rest)
- [Autenticación y seguridad](#autenticación-y-seguridad)
- [Base de datos y migraciones](#base-de-datos-y-migraciones)
- [Roles y permisos](#roles-y-permisos)

---

## Arquitectura

```
┌─────────────────┐     HTTP/JSON      ┌──────────────────────┐
│  Frontend        │ ──────────────▶  │  Backend (Spring Boot)│
│  React 18 + Vite │ ◀──────────────  │  Puerto 8080          │
│  Puerto 80       │  ApiResponse<T>   └──────────┬───────────┘
└─────────────────┘                              │ JDBC
                                                 ▼
                                    ┌──────────────────────┐
                                    │  PostgreSQL 17        │
                                    │  Puerto 5433 (host)   │
                                    └──────────────────────┘
```

**Stack:**
- **Backend:** Spring Boot 3.5.6, Java 17, Spring Security + JWT (JJWT 0.12.5), Spring Data JPA, Flyway
- **Frontend:** React 18, Vite, Material-UI (MUI), Axios
- **Base de datos:** PostgreSQL 17
- **Contenedores:** Docker Compose

---

## Requisitos previos

- Docker Desktop >= 24 y Docker Compose v2
- (Solo desarrollo local) JDK 17+, Node.js 20+, Maven 3.9+

---

## Inicio rápido con Docker

```bash
# 1. Copiar el archivo de variables de entorno y editarlo
cp .env.example .env
# Editar .env con tus credenciales reales

# 2. Levantar todos los servicios
docker compose up -d --build

# 3. Verificar que los tres servicios estén corriendo
docker compose ps
```

El frontend queda disponible en `http://localhost` y el backend en `http://localhost:8080`.

---

## Desarrollo local

### Backend

```bash
cd backend
export DB_URL=jdbc:postgresql://localhost:5433/asistencia_db
export DB_USERNAME=postgres
export DB_PASSWORD=tu_password
export JWT_SECRET=clave_de_al_menos_32_caracteres

mvn spring-boot:run
```

### Frontend

```bash
cd frontend
cp .env.example .env
# Editar VITE_API_URL si el backend corre en otro puerto

npm install
npm run dev
# Disponible en http://localhost:5173
```

---

## Variables de entorno

### `.env` (raíz del proyecto — para Docker Compose)

| Variable      | Descripción                                    | Ejemplo               |
|---------------|------------------------------------------------|-----------------------|
| `DB_NAME`     | Nombre de la base de datos                     | `asistencia_db`       |
| `DB_USERNAME` | Usuario de PostgreSQL                          | `postgres`            |
| `DB_PASSWORD` | Contraseña de PostgreSQL                       | `supersecret`         |
| `JWT_SECRET`  | Clave HMAC-SHA256 (minimo 32 caracteres)       | `cambia_en_produccion`|

### `frontend/.env`

| Variable       | Descripción                              | Ejemplo                      |
|----------------|------------------------------------------|------------------------------|
| `VITE_API_URL` | URL base del API (sin `/` final)         | `http://localhost:8080/api`  |

> **Nunca** commitear `.env` al repositorio. Está en `.gitignore`.

---

## Estructura del proyecto

```
sistema-asistencia/
├── backend/
│   ├── src/main/java/mx/gob/sedif/asistencia/
│   │   ├── core/
│   │   │   ├── area/            # Areas organizacionales
│   │   │   ├── asistencia/      # Registro, reportes, sanciones
│   │   │   ├── horario/         # Horarios y excepciones de horario
│   │   │   ├── justificacion/   # Catalogo de justificaciones
│   │   │   └── usuario/         # CRUD de usuarios
│   │   ├── exception/
│   │   │   ├── ApiResponse.java          # Wrapper uniforme de respuestas
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── MessageConstants.java     # Todos los mensajes al usuario
│   │   └── security/
│   │       ├── auth/            # Login, refresh token, logout
│   │       └── config/          # SecurityConfig, JwtFilter
│   └── src/main/resources/
│       ├── db/migration/        # Scripts Flyway (V1, V2, V3...)
│       └── application.properties
├── frontend/
│   └── src/
│       ├── components/          # DynamicTable, modales, formularios
│       ├── context/             # AuthContext, NotificationContext
│       ├── pages/               # Una pagina por modulo
│       └── services/            # Clientes HTTP por dominio
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## API REST

Todas las respuestas siguen el formato `ApiResponse<T>`:

```json
{
  "success": true,
  "code": 200,
  "message": "Operacion completada",
  "data": { ... },
  "fieldErrors": null,
  "timestamp": "2026-06-05T10:30:00"
}
```

En errores, `success` es `false` y `data` es `null`. Para errores de validacion, `fieldErrors` contiene la lista de campos invalidos.

### Endpoints principales

| Metodo | Ruta                                  | Rol minimo  | Descripcion                                      |
|--------|---------------------------------------|-------------|--------------------------------------------------|
| POST   | `/api/auth/login`                     | Publico     | Autenticacion con usuario y contrasena           |
| POST   | `/api/auth/identificar`               | Publico     | Identificacion por kiosco (sin contrasena)       |
| POST   | `/api/auth/refresh`                   | Publico     | Renueva access token con refresh token (cookie)  |
| POST   | `/api/auth/logout`                    | Autenticado | Invalida la cookie de refresh token              |
| GET    | `/api/asistencia/estado-diario`       | USER        | Estado de entrada/salida del dia actual          |
| POST   | `/api/asistencia/registrar-entrada`   | USER        | Registra entrada con foto base64                 |
| POST   | `/api/asistencia/registrar-salida`    | USER        | Registra salida con foto base64                  |
| GET    | `/api/asistencia/reporte`             | ADMIN       | Reporte paginado con filtros                     |
| POST   | `/api/asistencia/manual`              | ADMIN       | Crea registro manual                             |
| PUT    | `/api/asistencia/manual/{id}`         | ADMIN       | Edita registro manual                            |
| DELETE | `/api/asistencia/{id}`                | ADMIN       | Elimina registro                                 |
| GET    | `/api/asistencia/exportar/excel`      | ADMIN       | Descarga reporte en Excel                        |
| GET    | `/api/asistencia/exportar/pdf`        | ADMIN       | Descarga reporte en PDF                          |
| POST   | `/api/asistencia/upload`              | ADMIN       | Carga masiva desde Excel de biometrico           |
| GET    | `/api/asistencia/resumen-sanciones`   | ADMIN       | Sanciones calculadas por periodo                 |
| GET    | `/api/core/usuario`                   | ADMIN       | Lista paginada de usuarios                       |
| POST   | `/api/core/usuario`                   | ADMIN       | Crea usuario                                     |
| PUT    | `/api/core/usuario/{id}`              | ADMIN       | Actualiza usuario                                |
| DELETE | `/api/core/usuario/{id}`              | ADMIN       | Soft-delete de usuario                           |
| GET    | `/api/core/area`                      | ADMIN       | Lista de areas                                   |
| GET    | `/api/core/horario`                   | ADMIN       | Lista de horarios                                |
| GET    | `/api/core/justificacion`             | ADMIN       | Catalogo de justificaciones                      |

---

## Autenticacion y seguridad

El sistema usa **JWT + Refresh Token rotativo**:

1. **Login** el servidor retorna un `accessToken` (1 hora) en el body JSON y un `refreshToken` (7 dias) en una cookie `HttpOnly; Secure; Path=/api/auth`.
2. **Peticiones autenticadas** el frontend envia `Authorization: Bearer <accessToken>`.
3. **Token expirado (401)** el interceptor de Axios llama automaticamente a `/api/auth/refresh`. La cookie se envia automaticamente. El servidor valida el refresh token, rota la cookie y devuelve un nuevo access token.
4. **Logout** llama a `/api/auth/logout` que establece la cookie con `maxAge=0`.

**Flujo del kiosco:** usa `/api/auth/identificar` que acepta numero de control y foto sin contrasena. Genera los mismos tokens y cierra la sesion automaticamente al terminar el registro.

---

## Base de datos y migraciones

Las migraciones se ejecutan automaticamente al iniciar el backend con Flyway. Los scripts estan en `backend/src/main/resources/db/migration/`:

| Script | Descripcion |
|--------|-------------|
| `V1__create_initial_tables.sql` | Esquema inicial: usuarios, areas, horarios, asistencias, justificaciones |
| `V2__add_columna_requiere_cambio_password.sql` | Columna para forzar cambio de contrasena en primer login |
| `V3__cambiar_enums_a_string.sql` | Convierte columnas de rol y estatus de enteros a texto (migracion de EnumType.ORDINAL a STRING) |

---

## Roles y permisos

| Rol          | Descripcion                                                                  |
|--------------|------------------------------------------------------------------------------|
| `SUPERADMIN` | Acceso completo. Puede crear administradores y ver todos los reportes.       |
| `ADMIN`      | Gestion de usuarios de sus areas, reportes y justificaciones.                |
| `USER`       | Solo puede registrar asistencia y consultar su propio historial.             |

La contrasena por defecto de un usuario nuevo es `{numeroControl}-DIF`. El sistema obliga a cambiarla en el primer inicio de sesion.
