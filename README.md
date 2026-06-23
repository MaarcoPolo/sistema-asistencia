# Sistema de Control de Asistencia — SEDIF

Sistema web para el registro y control de asistencia del personal de la
Secretaría de Desarrollo e Inclusión Familiar (SEDIF). Los empleados registran
entrada y salida desde un kiosco con cámara; los administradores gestionan
usuarios, áreas, horarios y justificaciones, consultan reportes y exportan en
Excel/PDF.

Monorepo con tres servicios dockerizados: **frontend (React/Vite)**,
**backend (Spring Boot)** y **base de datos (PostgreSQL)**.

> Documentación específica de cada parte:
> - [`backend/README.md`](backend/README.md)
> - [`frontend/README.md`](frontend/README.md)

---

## Tabla de contenidos

- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Requisitos previos](#requisitos-previos)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Variables de entorno](#variables-de-entorno)
- [Levantar el proyecto en local](#levantar-el-proyecto-en-local)
- [Despliegue en servidor](#despliegue-en-servidor)
- [Migrar / restaurar la base de datos](#migrar--restaurar-la-base-de-datos)
- [Funcionalidades clave](#funcionalidades-clave)
- [Respaldos](#respaldos)
- [Roles](#roles)
- [Convenciones](#convenciones)

---

## Arquitectura

```
                  ┌──────────────────────────────────────────────────┐
   Navegador ───▶ │  Reverse proxy (Nginx Proxy Manager, contenedor)  │
                  │   /        → frontend                             │
                  │   /api/    → backend                              │
                  └───────┬───────────────────────┬──────────────────┘
                          │ (red docker)           │
                  ┌───────▼──────┐         ┌────────▼─────────┐      ┌──────────────┐
                  │  frontend    │         │   backend         │ JDBC │   db          │
                  │  React+Vite  │         │   Spring Boot     │────▶ │  PostgreSQL   │
                  │  Nginx :8080 │         │   :8080           │      │  :5432        │
                  └──────────────┘         └───────────────────┘      └──────────────┘
```

- El **frontend** y el **backend** se comunican bajo el mismo origen
  (`/` y `/api`), por lo que no hay problemas de CORS en producción.
- En el servidor, los contenedores **no publican puertos al host**: el reverse
  proxy los alcanza por nombre dentro de la red de Docker.

---

## Tecnologías

| Capa | Stack |
|---|---|
| Frontend | React 19, Vite 7, Material UI 7, axios, react-router-dom 7 |
| Backend | Java 17, Spring Boot 3.5.6 (Web, Data JPA, Security, Validation, Cache, Actuator) |
| Seguridad | JWT (jjwt 0.12.5), BCrypt, rate limiting (bucket4j) |
| Base de datos | PostgreSQL 17 + Flyway (migraciones) |
| Exportación | Apache POI (Excel), OpenPDF (PDF) |
| Contenedores | Docker + Docker Compose |
| Reverse proxy | Nginx Proxy Manager (en servidor) |

---

## Requisitos previos

- **Docker** y **Docker Compose** (v2).
- Para desarrollo sin Docker: **Java 17**, **Node 18+** y **PostgreSQL**.

---

## Estructura del repositorio

```
sistema-asistencia/
├── backend/                    # API Spring Boot (ver backend/README.md)
├── frontend/                   # SPA React/Vite (ver frontend/README.md)
├── docker-compose.yml          # Base: servicios db / backend / frontend (sin puertos)
├── docker-compose.local.yml    # Override LOCAL: publica puertos para tu PC
├── docker-compose.prod.yml     # Override SERVIDOR: conecta a la red del reverse proxy
├── .env.example                # Plantilla de variables (copiar a .env)
└── .gitignore
```

### ¿Por qué tres archivos compose?

El `docker-compose.yml` base **no publica puertos** (eso depende del entorno).
Se combina con un override según dónde levantes:

- **Local** → `docker-compose.local.yml` (expone puertos a tu máquina).
- **Servidor** → `docker-compose.prod.yml` (sin puertos; usa la red de Nginx
  Proxy Manager y construye el frontend con `VITE_API_URL=/api`).

---

## Variables de entorno

Copia la plantilla y rellena los valores reales (el `.env` **no** se commitea):

```bash
cp .env.example .env
```

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_NAME` | Nombre de la base de datos | `asistencia_db` |
| `DB_USERNAME` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | (secreto) |
| `JWT_SECRET` | Secreto del JWT (≥ 64 caracteres). `openssl rand -base64 64` | (secreto largo) |
| `CORS_ALLOWED_ORIGIN` | Origen del frontend (servidor/prod), sin barra final | `http://asistencia.sedif.gob.mx` |

> El `.env` lo lee automáticamente Docker Compose. **Nunca** lo subas al repo
> (está en `.gitignore`); `.env.example` es la plantilla versionada.

---

## Levantar el proyecto en local

```bash
# 1. Variables de entorno
cp .env.example .env          # edita los valores

# 2. Levantar todo (base + override local)
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
```

Con el override local, los servicios quedan accesibles en tu PC:

| Servicio | URL local |
|---|---|
| Frontend | http://localhost |
| Backend (API) | http://localhost:8080/api |
| PostgreSQL | localhost:5433 |

Comandos útiles:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml ps
docker compose -f docker-compose.yml -f docker-compose.local.yml logs -f backend
docker compose -f docker-compose.yml -f docker-compose.local.yml down
```

> Alternativa sin Docker: ver los README de `backend/` y `frontend/` para
> ejecutar cada uno por separado (`./mvnw spring-boot:run` y `npm run dev`).

---

## Despliegue en servidor

El servidor usa **Nginx Proxy Manager (NPM)** como reverse proxy (contenedor).

```bash
# En el servidor, dentro del directorio del proyecto:
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Para no escribir los dos `-f` cada vez, en el servidor se fija el archivo compose:

```bash
echo 'export COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml' >> ~/.bashrc
source ~/.bashrc
# luego basta:  docker compose up -d --build
```

### Reverse proxy (Nginx Proxy Manager)

NPM se administra desde su **interfaz web** (puerto `81` por defecto), no por
archivos de configuración. Pasos para publicar el sistema:

1. **Hosts → Proxy Hosts → Add Proxy Host.** Pestaña **Details**:
   - **Domain Names**: el dominio del sistema (ej. `asistencia.sedif.gob.mx`).
   - **Scheme**: `http`
   - **Forward Hostname / IP**: `asistencia-frontend`
   - **Forward Port**: `8080`
   - Activar **Block Common Exploits**.

2. Pestaña **Advanced** → en *Custom Nginx Configuration* pegar el ruteo de la
   API y el tamaño máximo de subida (el backend acepta archivos de hasta ~6 MB):

   ```nginx
   client_max_body_size 8m;

   location /api/ {
       proxy_pass http://asistencia-backend:8080;
       proxy_set_header Host $host;
       proxy_set_header X-Real-IP $remote_addr;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header X-Forwarded-Proto $scheme;
   }
   ```

   > Importante: el ruteo de `/api` va en **Advanced**, NO en la pestaña
   > "Custom Locations" del formulario — esa última reescribe mal la ruta y
   > produce **405** en el login.

3. **Save.**

Para que NPM resuelva `asistencia-frontend` / `asistencia-backend` por nombre,
los contenedores deben estar en la **misma red de Docker que NPM** (declarada
como red externa en `docker-compose.prod.yml`). Verificación:

```bash
docker network inspect <red_de_npm> --format '{{range .Containers}}{{.Name}}{{"\n"}}{{end}}'
# deben aparecer: el contenedor de NPM, asistencia-frontend y asistencia-backend
```

> En pruebas el acceso puede ser por HTTP sin SSL. Para producción real conviene
> habilitar HTTPS (Let's Encrypt) desde la pestaña **SSL** del Proxy Host.

---

## Migrar / restaurar la base de datos

El esquema lo gestiona **Flyway**. Para llevar datos existentes a otro entorno,
el orden importa por la tabla `flyway_schema_history`:

```bash
# 1. [ORIGEN] generar el dump (dentro del contenedor de la BD)
docker exec -t asistencia-db pg_dump -U postgres -d asistencia_db --clean --if-exists > dump.sql

# 2. transferir el dump al destino (scp, etc.)

# 3. [DESTINO] levantar SOLO la BD y esperar "healthy"
docker compose up -d db
docker compose ps

# 4. restaurar el dump ANTES de arrancar el backend
docker exec -i asistencia-db psql -U postgres -d asistencia_db < dump.sql

# 5. levantar el resto
docker compose up -d --build
```

> Restaurar **antes** del backend evita que Flyway intente crear tablas que el
> dump ya trae (el dump incluye el esquema y el historial de migraciones).

---

## Funcionalidades clave

- **Kiosco con cámara**: el empleado registra entrada/salida con foto.
- **Panel de administración**: gestión de usuarios, áreas, horarios y catálogo
  de justificaciones.
- **Reportes de asistencia** con filtros y exportación a **Excel** y **PDF**.
- **Exportación a Excel** en los catálogos (usuarios, áreas, horarios,
  justificaciones); en usuarios, con modal de opciones (todos / por área / uno).
- **Carga masiva de usuarios** desde Excel, con validación fila por fila y
  reporte de errores (duplicados, área inexistente, datos faltantes).
- **Justificaciones** de incidencias (flujo de solicitud y aprobación).
- **Cálculo de sanciones** y descuentos por incidencias.

---

## Respaldos

Respaldo manual de la base de datos:

```bash
docker exec -t asistencia-db pg_dump -U postgres -d asistencia_db --clean --if-exists \
  > backup_$(date +%F).sql
```

Recomendado automatizarlo con `cron` en el servidor, guardando los dumps **fuera**
del repositorio (los `*.sql` están ignorados por git salvo las migraciones).

---

## Roles

| Rol | Alcance |
|---|---|
| `SUPERADMIN` | Acceso completo: todas las áreas y operaciones; crea administradores |
| `ADMIN` | Solo las áreas que gestiona; no crea usuarios con rol superior |
| `USER` | Registra su asistencia y consulta su propio historial / justifica lo propio |

> La contraseña por defecto de un usuario nuevo es `{numeroControl}-DIF`. El
> sistema obliga a cambiarla en el primer inicio de sesión.

---

## Convenciones

- **Backend organizado por feature** (`core/<feature>/`), no por capas. Detalle
  en `backend/README.md`.
- **Frontend**: toda llamada al backend pasa por `services/`; UI con MUI; estilo
  centralizado en `theme.js`. Detalle en `frontend/README.md`.
- **Secretos**: solo en `.env` (ignorado por git); `.env.example` es la plantilla.
- **Migraciones**: nunca editar una ya aplicada; crear `V(n+1)__...`.
