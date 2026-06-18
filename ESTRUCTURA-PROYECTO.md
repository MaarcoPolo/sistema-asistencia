# Manual de estructura de proyecto

Guía para replicar la organización de carpetas y archivos de este proyecto en
nuevos desarrollos. El objetivo es que todos los proyectos del equipo se vean y
se trabajen igual: backend Spring Boot (Java 17) + frontend React/Vite, ambos
dockerizados y orquestados con Docker Compose.

> **Convención central:** el backend NO se organiza por capas técnicas
> (`controllers/`, `services/`, `repositories/` por separado), sino **por
> dominio / feature**. Cada feature vive en su propia carpeta con TODAS sus
> clases juntas (entidad, repositorio, servicio, controlador, DTOs). Esto es lo
> más importante a respetar.

---

## 1. Estructura de la raíz del repositorio

```
mi-proyecto/
├── .vscode/                    # Config del editor (opcional, compartida)
├── backend/                    # Aplicación Spring Boot (ver sección 2)
├── frontend/                   # Aplicación React + Vite (ver sección 3)
├── deploy/                     # Archivos de despliegue (ej. config de proxy)
├── .env                        # Credenciales reales (NUNCA se sube a git)
├── .env.example                # Plantilla de variables, con valores ficticios
├── .gitignore
├── README.md                   # Documentación principal del proyecto
├── docker-compose.yml          # Base: define los 3 servicios SIN puertos
├── docker-compose.local.yml    # Override para desarrollo local (publica puertos)
└── docker-compose.prod.yml     # Override para servidor (red del reverse proxy)
```

### Archivos de la raíz — qué va en cada uno

| Archivo | Contenido |
|---|---|
| `docker-compose.yml` | Definición común de los servicios `db`, `backend`, `frontend`. No publica puertos: eso depende del entorno. |
| `docker-compose.local.yml` | Repone los puertos para acceder desde tu PC (`5433`, `8080`, `80`). Se usa solo en desarrollo. |
| `docker-compose.prod.yml` | Conecta backend y frontend a la red del reverse proxy (NPM) y pasa `VITE_API_URL=/api` al build. No publica puertos. |
| `.env.example` | Plantilla con todas las variables que el proyecto necesita, con valores de ejemplo. SÍ se sube a git. |
| `.env` | Copia del anterior con los valores reales. **Va en `.gitignore`, nunca se commitea.** |

---

## 2. Backend (Spring Boot, Java 17)

### 2.1 Raíz del módulo `backend/`

```
backend/
├── .mvn/wrapper/               # Maven Wrapper (no tocar)
├── src/                        # Código fuente (ver abajo)
├── Dockerfile                  # Build multi-etapa: Maven build -> JRE alpine, usuario no-root
├── mvnw  /  mvnw.cmd           # Maven Wrapper (Linux / Windows)
└── pom.xml                     # Dependencias y configuración Maven
```

### 2.2 Árbol del código fuente

El paquete base es `mx.gob.sedif.asistencia` — **en un proyecto nuevo se cambia
por el paquete que corresponda** (ej. `mx.gob.sedif.<nombreproyecto>`), pero la
estructura interna se mantiene idéntica.

```
src/
├── main/
│   ├── java/mx/gob/sedif/asistencia/
│   │   ├── <App>Application.java        # Clase main de Spring Boot (punto de arranque)
│   │   │
│   │   ├── config/                      # Configuración transversal de la app
│   │   │   ├── SecurityConfig.java      #   - cadena de seguridad / filtros
│   │   │   ├── WebConfig.java           #   - CORS, MVC, etc.
│   │   │   └── CacheConfig.java         #   - configuración de caché
│   │   │
│   │   ├── core/                        # ★ LÓGICA DE NEGOCIO, ORGANIZADA POR FEATURE
│   │   │   ├── usuario/                 #   Cada subcarpeta = un dominio/feature
│   │   │   ├── area/
│   │   │   ├── asistencia/
│   │   │   ├── horario/
│   │   │   └── justificacion/
│   │   │
│   │   ├── security/                    # Todo lo relacionado a autenticación/autorización
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthFilter.java
│   │   │   ├── RateLimitingFilter.java
│   │   │   ├── SecurityUtil.java
│   │   │   ├── UserDetailsServiceImpl.java
│   │   │   └── auth/                     #   Endpoints y DTOs del login/identificación
│   │   │       ├── AuthController.java
│   │   │       ├── LoginRequest.java
│   │   │       └── JwtResponse.java
│   │   │
│   │   ├── exception/                   # Manejo global de errores y respuestas estándar
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── ApiResponse.java         #   Envoltorio de respuesta uniforme { data, message }
│   │   │   └── MessageConstants.java
│   │   │
│   │   └── util/                        # Utilidades compartidas
│   │       ├── Audit.java / Auditable.java / AuditableListener.java
│   │       └── enums/                   #   Enumeraciones del dominio
│   │           ├── Rol.java
│   │           ├── Estado.java
│   │           └── EstatusJustificacion.java
│   │
│   └── resources/
│       ├── application.properties       # Config base (desarrollo)
│       ├── application-prod.properties  # Overrides para perfil 'prod'
│       └── db/migration/                # Migraciones Flyway (se ejecutan en orden)
│           ├── V1__create_initial_tables.sql
│           ├── V2__add_columna_xxx.sql
│           └── V3__...                  #   Vn__descripcion.sql  (numeración incremental)
│
└── test/
    └── java/mx/gob/sedif/asistencia/    # Tests, espejando la estructura de main/
        └── core/<feature>/              #   ej. core/asistencia/AsistenciaServiceTest.java
```

### 2.3 ★ Cómo se organiza una feature dentro de `core/`

Esta es la regla más importante. **Cada feature es una carpeta autocontenida**
que agrupa todas las clases de ese dominio. Ejemplo real de `core/area/`:

```
core/area/
├── Area.java                   # @Entity   — la entidad JPA (tabla)
├── AreaRepository.java         # @Repository — acceso a datos (extiende JpaRepository)
├── AreaService.java            # @Service  — lógica de negocio
├── AreaResource.java           # @RestController — endpoints REST (la "C" del MVC)
└── AreaRecord.java             # DTO (Java record) — datos que entran/salen por la API
```

**Patrón de nombres a respetar en cada feature:**

| Sufijo | Rol | Anotación |
|---|---|---|
| `<Nombre>.java` | Entidad JPA | `@Entity` |
| `<Nombre>Repository.java` | Acceso a datos | `@Repository` / extiende `JpaRepository` |
| `<Nombre>Service.java` | Lógica de negocio | `@Service` |
| `<Nombre>Resource.java` | Controlador REST | `@RestController` |
| `<Nombre>Record.java` | DTO de salida | `record` de Java |
| `<Accion>Request.java` | DTO de entrada | `record` de Java |

> Nota: en este proyecto los controladores se llaman `...Resource` (estilo JAX-RS).
> Mantén ese sufijo para ser consistente. (La excepción histórica es
> `AuthController` dentro de `security/auth/`.)

**Para crear una feature nueva** (ej. "departamento"): crea
`core/departamento/` y dentro `Departamento.java`, `DepartamentoRepository.java`,
`DepartamentoService.java`, `DepartamentoResource.java` y los records que necesite.
No se reparten en carpetas técnicas separadas.

---

## 3. Frontend (React + Vite)

### 3.1 Raíz del módulo `frontend/`

```
frontend/
├── public/                     # Estáticos servidos tal cual (no pasan por el bundler)
│   ├── vite.svg
│   └── assets/                 #   imágenes/logos (ej. .webp, .png)
├── src/                        # Código de la app (ver abajo)
├── Dockerfile                  # Build multi-etapa: Vite build -> Nginx (sin root, puerto 8080)
├── nginx.conf                  # Config del Nginx interno del contenedor (SPA + headers)
├── index.html                  # HTML raíz que monta la app
├── package.json                # Dependencias y scripts npm
├── package-lock.json
├── vite.config.js              # Configuración de Vite (build, chunks)
├── eslint.config.js            # Reglas de linting
└── README.md
```

### 3.2 Árbol de `src/`

```
src/
├── main.jsx                    # Punto de entrada: monta <App> en el DOM
├── App.jsx                     # Componente raíz: define el enrutamiento (rutas)
├── App.css / index.css         # Estilos globales
├── theme.js                    # Tema de MUI (colores, tipografía)
│
├── assets/                     # Estáticos importados desde el código (procesados por Vite)
│
├── pages/                      # ★ Una pantalla/ruta completa por archivo
│   ├── LoginAdmin.jsx
│   ├── AdminDashboard.jsx
│   ├── AdminUsuarios.jsx
│   ├── AdminAreas.jsx
│   ├── AdminHorarios.jsx
│   ├── AdminJustificaciones.jsx
│   ├── MisAsistencias.jsx
│   ├── PaginaAsistencia.jsx
│   └── IdentificacionUsuario.jsx
│
├── components/                 # ★ Componentes reutilizables (no son una ruta entera)
│   ├── AdminLayout.jsx / PublicLayout.jsx   # Layouts (estructura de página)
│   ├── ProtectedRoute.jsx                   # Guard de rutas autenticadas
│   ├── DynamicTable.jsx / TableSkeleton.jsx # Tabla genérica y su esqueleto de carga
│   ├── <Entidad>Form.jsx                    # Formularios (UsuarioForm, AreaForm, HorarioForm...)
│   ├── <Algo>Modal.jsx                      # Diálogos modales (ReporteModal, JustificarModal...)
│   ├── ConfirmationDialog.jsx
│   ├── Clock.jsx / Footer.jsx
│   └── ...
│
├── context/                    # React Context (estado global)
│   ├── AuthContext.jsx         #   sesión / usuario autenticado
│   └── NotificationContext.jsx #   notificaciones / toasts
│
└── services/                   # ★ Capa de comunicación con el backend (axios)
    ├── api.js                  #   instancia base de axios (baseURL = VITE_API_URL, interceptores)
    ├── authService.js          #   un servicio por dominio, espeja las features del backend
    ├── usuarioService.js
    ├── areaService.js
    ├── asistenciaService.js
    ├── horarioService.js
    └── justificacionService.js
```

### 3.3 Reglas del frontend a respetar

| Carpeta | Qué va aquí | Qué NO va aquí |
|---|---|---|
| `pages/` | Componentes que representan una **ruta** completa (una pantalla). | Piezas pequeñas reutilizables. |
| `components/` | Piezas reutilizables: formularios, modales, tablas, layouts. | Lógica de llamadas HTTP. |
| `services/` | **Toda** llamada al backend. Un archivo `xxxService.js` por dominio. | JSX / componentes. |
| `context/` | Estado global compartido entre muchas pantallas. | Estado local de un componente. |

**Patrón clave:** los componentes y páginas **nunca llaman a axios directamente**.
Siempre pasan por un servicio de `services/`. Y `services/api.js` centraliza la
`baseURL` (que se lee de `VITE_API_URL`) y los interceptores. Un dominio nuevo en
el backend (ej. "departamento") tendría su `departamentoService.js` espejo.

---

## 4. Correspondencia backend ↔ frontend

La organización por feature se refleja en ambos lados. Para un dominio nuevo
llamado, por ejemplo, `reporte`:

| Capa | Backend | Frontend |
|---|---|---|
| Datos / API | `core/reporte/Reporte*.java` | `services/reporteService.js` |
| Pantalla | (endpoints en `ReporteResource`) | `pages/AdminReportes.jsx` |
| UI auxiliar | — | `components/ReporteForm.jsx`, `components/ReporteModal.jsx` |

---

## 5. Checklist para arrancar un proyecto nuevo con esta estructura

```
[ ] Crear raíz con: backend/  frontend/  deploy/
[ ] Copiar los 3 docker-compose (base, local, prod) y el .env.example
[ ] Backend:
      [ ] pom.xml + Dockerfile + Maven wrapper
      [ ] paquete base propio (mx.gob.sedif.<proyecto>)
      [ ] carpetas: config/  core/  security/  exception/  util/
      [ ] resources/: application.properties, application-prod.properties, db/migration/
      [ ] primera migración V1__create_initial_tables.sql
[ ] Frontend:
      [ ] package.json + vite.config.js + Dockerfile + nginx.conf + index.html
      [ ] src/: main.jsx, App.jsx, theme.js
      [ ] carpetas: pages/  components/  context/  services/
      [ ] services/api.js con la baseURL desde VITE_API_URL
[ ] Por cada feature nueva:
      [ ] backend: core/<feature>/ con Entity, Repository, Service, Resource, Records
      [ ] frontend: services/<feature>Service.js + pages/ + components/ necesarios
```
```
