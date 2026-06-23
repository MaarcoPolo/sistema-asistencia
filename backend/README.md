# Backend — Sistema de Control de Asistencia (SEDIF)

API REST en **Spring Boot (Java 17)** que da servicio al sistema de asistencia:
registro de entradas/salidas, gestión de usuarios, áreas, horarios y
justificaciones, reportes y exportaciones (Excel/PDF). Autenticación sin estado
con **JWT** (access + refresh), autorización por roles y persistencia en
**PostgreSQL** con migraciones **Flyway**.

---

## Tabla de contenidos

- [Tecnologías](#tecnologías)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Cómo ejecutar](#cómo-ejecutar)
- [Variables de entorno](#variables-de-entorno)
- [Perfiles de configuración](#perfiles-de-configuración)
- [Seguridad](#seguridad)
- [Contrato de API (ApiResponse)](#contrato-de-api-apiresponse)
- [Manejo de errores](#manejo-de-errores)
- [Endpoints principales](#endpoints-principales)
- [Base de datos y migraciones](#base-de-datos-y-migraciones)
- [Roles](#roles)

---

## Tecnologías

| Componente | Versión / detalle |
|---|---|
| Java | 17 |
| Spring Boot | 3.5.6 (parent) |
| Módulos Spring | Web, Data JPA, Security, Validation, Cache, Actuator |
| Base de datos | PostgreSQL 17 |
| Migraciones | Flyway (`flyway-core` + `flyway-database-postgresql`) |
| JWT | jjwt 0.12.5 (api / impl / jackson) |
| Rate limiting | bucket4j 8.14.0 (`bucket4j_jdk17-core`) |
| Caché | Caffeine |
| Excel | Apache POI (`poi-ooxml` 5.2.5) |
| PDF | OpenPDF (`com.github.librepdf` / lowagie) |
| Utilidades | Lombok |
| Build | Maven (con wrapper `mvnw`) |

---

## Estructura del proyecto

El backend **no se organiza por capas técnicas** (`controllers/`, `services/`
sueltos), sino **por dominio / feature**. Cada feature agrupa todas sus clases.

```
src/main/java/mx/gob/sedif/asistencia/
├── AsistenciaBackendApplication.java   # Punto de arranque
│
├── config/                             # Configuración transversal
│   ├── SecurityConfig.java             #   cadena de filtros, BCrypt, headers
│   ├── WebConfig.java                  #   CORS
│   └── CacheConfig.java                #   caché Caffeine
│
├── core/                               # ★ Lógica de negocio por feature
│   ├── usuario/                        #   Usuario, Repository, Service, Resource, Records
│   ├── area/
│   ├── asistencia/                     #   incluye reportes (Excel/PDF) y carga masiva
│   ├── horario/                        #   Horario + HorarioDetalle + excepciones
│   └── justificacion/                  #   catálogo + justificaciones de asistencia
│
├── security/                           # Autenticación / autorización
│   ├── JwtTokenProvider.java           #   genera/valida tokens
│   ├── JwtAuthFilter.java              #   filtro que valida el access token
│   ├── RateLimitingFilter.java         #   límite de intentos en login
│   ├── SecurityUtil.java               #   usuario autenticado actual
│   ├── JwtSecretValidator.java         #   valida el secreto al arranque
│   ├── UserDetailsServiceImpl.java
│   └── auth/                           #   AuthController, LoginRequest, JwtResponse, ...
│
├── exception/                          # Contrato de respuesta y errores
│   ├── ApiResponse.java                #   envoltorio uniforme de respuestas
│   ├── GlobalExceptionHandler.java     #   manejo centralizado de excepciones
│   └── MessageConstants.java           #   mensajes de usuario
│
└── util/                               # Utilidades compartidas
    ├── Audit / Auditable / AuditableListener   # auditoría de entidades
    ├── ExportExcelService.java         #   generación genérica de Excel (POI)
    └── enums/                          #   Rol, Estado, EstatusJustificacion

src/main/resources/
├── application.properties              # Configuración base (desarrollo)
├── application-prod.properties         # Overrides del perfil 'prod'
└── db/migration/                       # Migraciones Flyway (V1__... V5__...)
```

### Patrón de cada feature (`core/<feature>/`)

| Sufijo | Rol | Anotación |
|---|---|---|
| `<Nombre>.java` | Entidad JPA | `@Entity` |
| `<Nombre>Repository.java` | Acceso a datos | `@Repository` / `JpaRepository` |
| `<Nombre>Service.java` | Lógica de negocio | `@Service` |
| `<Nombre>Resource.java` | Controlador REST | `@RestController` |
| `<Nombre>Record.java` | DTO de salida | `record` |
| `<Accion>Request.java` | DTO de entrada | `record` |

> Los controladores usan el sufijo **`Resource`** (no `Controller`), salvo
> `AuthController` por convención del módulo de seguridad.

---

## Cómo ejecutar

### Con Docker (recomendado)

Desde la **raíz del repositorio** (no desde `backend/`), porque la base de datos
y el resto del stack se orquestan con Docker Compose. Ver el README de la raíz.

### Local (solo el backend)

Requiere Java 17 y un PostgreSQL accesible.

```bash
# Desde backend/
./mvnw spring-boot:run          # Linux / Mac
mvnw.cmd spring-boot:run        # Windows

# Compilar el .jar
./mvnw clean package
java -jar target/*.jar
```

Las variables de conexión y JWT se leen del entorno (ver abajo).

---

## Variables de entorno

| Variable | Descripción |
|---|---|
| `DB_URL` | URL JDBC, ej. `jdbc:postgresql://db:5432/asistencia_db` |
| `DB_USERNAME` | Usuario de PostgreSQL |
| `DB_PASSWORD` | Contraseña de PostgreSQL |
| `JWT_SECRET` | Secreto HMAC del JWT (mín. 64 caracteres). Generar con `openssl rand -base64 64` |
| `CORS_ALLOWED_ORIGIN` | Origen permitido del frontend (ej. `http://asistencia.sedif.gob.mx`) |
| `SPRING_PROFILES_ACTIVE` | `prod` en servidor |
| `JAVA_OPTS` | Ajustes de JVM (memoria/GC) en contenedor |

El secreto y las credenciales **nunca** se definen en código; provienen del
entorno (en Docker, del archivo `.env` de la raíz).

---

## Perfiles de configuración

- **`application.properties`** — base, pensada para desarrollo.
- **`application-prod.properties`** — se activa con `SPRING_PROFILES_ACTIVE=prod`.
  Endurece producción: `show-sql=false`, logs en WARN para Spring/Security,
  pool HikariCP afinado, caché Caffeine, Actuator solo `health` sin detalles,
  `server.error.include-stacktrace=never`, `include-message=never`, y
  `server.forward-headers-strategy=framework` (para obtener IP/esquema reales
  detrás del reverse proxy).

---

## Seguridad

- **Contraseñas**: BCrypt. Nunca se serializan en las respuestas (el DTO de
  salida lleva `password = null`).
- **JWT stateless**:
  - **Access token** (~1 h) en header `Authorization: Bearer ...`, con claim `roles`.
  - **Refresh token** (~7 d) en **cookie HttpOnly**; solo sirve para renovar el access.
  - Propiedades: `app.jwt.secret`, `app.jwt.expiration-ms`, `app.jwt.refresh-expiration-ms`.
  - El secreto se valida al arranque (`JwtSecretValidator`).
- **Filtros** (antes de `UsernamePasswordAuthenticationFilter`):
  `RateLimitingFilter` → `JwtAuthFilter`.
- **Rate limiting**: bucket de tokens por (IP + ruta), **5 intentos por minuto**,
  aplicado solo a `login` / `identificar`.
- **CORS** (`WebConfig`): mapea `/api/**`, origen desde `app.cors.allowed-origin`
  (nunca `*` con credenciales), `allowCredentials(true)`.
- **Headers** (`SecurityConfig`): `frameOptions.deny()`, `contentTypeOptions`,
  HSTS y `Content-Security-Policy`.
- **Sesión** `STATELESS`, CSRF deshabilitado (API con JWT).
- **Autorización por rol** en endpoints con `@PreAuthorize("hasAnyRole(...)")`.

---

## Contrato de API (ApiResponse)

Todas las respuestas se envuelven en `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Lista de usuarios obtenida correctamente",
  "data": { "...": "contenido real" }
}
```

Los controladores devuelven `ResponseEntity<ApiResponse<...>>`. Códigos:
GET → 200, POST → 201, PUT → 200, DELETE → 204.

---

## Manejo de errores

Centralizado en `GlobalExceptionHandler`:

| Excepción | HTTP |
|---|---|
| `MethodArgumentNotValidException` | 400 (+ lista de `FieldError`) |
| `IllegalArgumentException` | 400 |
| `IllegalStateException` | 409 |
| `SecurityException` / `AccessDeniedException` | 403 |
| `RuntimeException` / `Exception` | 500 (genérico, sin filtrar detalles internos) |

---

## Endpoints principales

> Base: todos bajo `/api`. La mayoría requiere JWT; `/api/auth/**` es público.

### Autenticación — `/api/auth`
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/login` | Login con número de control y contraseña |
| POST | `/identificar` | Identificación rápida (kiosco) |
| POST | `/refresh` | Renueva el access token (usa cookie de refresh) |
| POST | `/logout` | Cierra sesión |
| POST | `/reset-password` | Restablecimiento de contraseña |

### Usuarios — `/api/core/usuario`
CRUD + `GET /exportar/excel` (con filtros), `POST /carga-masiva` (Excel),
`GET /mi-perfil`, `POST /{id}/reset-password`, `POST /mi-contrasena`.

### Áreas — `/api/core/area`
CRUD + `GET /select-list` + `GET /exportar/excel`.

### Horarios — `/api/core/horario`
CRUD + `GET /exportar/excel`. Excepciones de horario en `/api/core/excepciones`.

### Justificaciones — `/api/core/justificacion`
CRUD + `GET /select-list` + `GET /exportar/excel`.

### Asistencias — `/api/asistencia`
Registro (`/registrar-entrada`, `/registrar-salida`, `/estado-diario`),
reporte paginado, registro manual (CRUD), exportación Excel/PDF, carga masiva
(`/upload`), sanciones, justificación (admin y propia), historial
(`/mis-asistencias`), y fotos.

---

## Base de datos y migraciones

- Esquema gestionado por **Flyway**. Las migraciones viven en
  `src/main/resources/db/migration/` con el formato `V<n>__descripcion.sql`.
- Se aplican automáticamente al arrancar el backend. **Nunca** edites una
  migración ya aplicada: crea una nueva `V(n+1)__...`.
- La tabla `flyway_schema_history` registra lo aplicado (relevante al restaurar
  respaldos: ver README de la raíz).

---

## Roles

| Rol | Alcance |
|---|---|
| `SUPERADMIN` | Acceso total a todas las áreas y operaciones |
| `ADMIN` | Solo las áreas que gestiona; no crea usuarios con rol superior |
| `USER` | Registra su asistencia y consulta su historial / justifica lo propio |

Estados de registro (`Estado`): `ACTIVE`, `INACTIVE`, `DELETED` (borrado lógico).
