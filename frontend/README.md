# Frontend — Sistema de Control de Asistencia (SEDIF)

Interfaz web en **React 19 + Vite** del sistema de asistencia. Incluye el panel
de administración (usuarios, áreas, horarios, justificaciones, reportes) y las
vistas públicas/empleado (login, kiosco con cámara para checar, historial
personal). UI construida con **Material UI (MUI)**.

---

## Tabla de contenidos

- [Tecnologías](#tecnologías)
- [Regla de dependencias](#regla-de-dependencias)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Cómo ejecutar](#cómo-ejecutar)
- [Variables de entorno](#variables-de-entorno)
- [Comunicación con el backend](#comunicación-con-el-backend)
- [Diseño y sistema visual](#diseño-y-sistema-visual)
- [Rutas](#rutas)
- [Build de producción y Docker](#build-de-producción-y-docker)

---

## Tecnologías

| Componente | Versión / detalle |
|---|---|
| React | 19 |
| Build / dev server | Vite 7 |
| UI | Material UI (`@mui/material`, `@mui/icons-material` 7) + Emotion |
| Routing | react-router-dom 7 |
| HTTP | axios |
| JWT (decodificar) | jwt-decode |
| Cámara (kiosco) | react-webcam |
| Utilidad | lodash.debounce |
| Lint | ESLint 9 |

---

## Regla de dependencias

> **MUI debe cubrir todas las necesidades de UI.** Solo se agrega una
> dependencia nueva cuando MUI no puede resolver algo **y** esa dependencia
> mejora notablemente el frontend o la experiencia de usuario, exclusivamente
> para casos complejos.

Por eso la responsividad, el tema, las tablas adaptables y las microinteracciones
se resuelven con MUI + `useMediaQuery` (incluido en MUI), sin librerías extra.
Las únicas dependencias fuera de MUI son funcionales y justificadas
(`axios`, `react-router-dom`, `react-webcam`, `jwt-decode`, `lodash.debounce`).

---

## Estructura del proyecto

```
frontend/
├── public/                     # Estáticos servidos tal cual (logos, íconos)
├── index.html                  # HTML raíz
├── nginx.conf                  # Config de Nginx para servir la SPA en producción
├── Dockerfile                  # Build multi-stage (Vite → Nginx no-root, puerto 8080)
├── vite.config.js              # Config de Vite (build, chunks manuales)
├── eslint.config.js
└── src/
    ├── main.jsx                # Punto de entrada (monta <App>, tema, contextos)
    ├── App.jsx                 # Enrutamiento + carga diferida (lazy) de páginas
    ├── theme.js                # ★ Sistema visual centralizado (MUI theme)
    │
    ├── pages/                  # Una pantalla/ruta por archivo
    │   ├── LoginAdmin.jsx
    │   ├── IdentificacionUsuario.jsx   # kiosco: identificación rápida
    │   ├── PaginaAsistencia.jsx        # kiosco: cámara para checar
    │   ├── MisAsistencias.jsx          # historial del empleado
    │   ├── AdminDashboard.jsx          # registro de asistencias
    │   ├── AdminUsuarios.jsx
    │   ├── AdminAreas.jsx
    │   ├── AdminHorarios.jsx
    │   └── AdminJustificaciones.jsx
    │
    ├── components/             # Reutilizables (no son una ruta entera)
    │   ├── AdminLayout.jsx     #   marco del panel (sidebar; sin header)
    │   ├── PublicLayout.jsx    #   marco de login/kiosco
    │   ├── ProtectedRoute.jsx  #   guard de rutas autenticadas
    │   ├── DynamicTable.jsx    #   tabla genérica (responsiva: tarjetas en móvil)
    │   ├── *Form.jsx           #   formularios (Usuario, Área, Horario, Asistencia)
    │   ├── *Modal.jsx          #   diálogos (Exportar, Carga masiva, Justificar, ...)
    │   ├── Clock.jsx / Footer.jsx
    │   └── ...
    │
    ├── context/               # Estado global
    │   ├── AuthContext.jsx     #   sesión, login/logout, refresh
    │   └── NotificationContext.jsx  # notificaciones / toasts
    │
    └── services/              # Capa HTTP (toda llamada al backend pasa por aquí)
        ├── api.js             #   instancia axios (baseURL, interceptores)
        ├── downloadHelper.js  #   descarga de archivos (blobs: Excel/PDF)
        ├── authService.js
        ├── usuarioService.js
        ├── areaService.js
        ├── horarioService.js
        ├── justificacionService.js
        └── asistenciaService.js
```

### Convenciones

- `pages/` = rutas completas; `components/` = piezas reutilizables;
  `context/` = estado global; `services/` = comunicación con el backend.
- **Ningún componente llama a axios directo**: siempre a través de un servicio.
- Un `xxxService.js` por dominio, espejo de las features del backend.

---

## Cómo ejecutar

```bash
# Instalar dependencias
npm install

# Servidor de desarrollo (Vite, con HMR)
npm run dev

# Lint
npm run lint

# Build de producción (genera dist/)
npm run build

# Previsualizar el build
npm run preview
```

> Para levantar el sistema completo (frontend + backend + base de datos) usa
> Docker desde la raíz del repo; ver el README de la raíz.

---

## Variables de entorno

| Variable | Descripción |
|---|---|
| `VITE_API_URL` | URL base de la API. En desarrollo, suele ser `http://localhost:8080/api`. En producción se inyecta como **build-arg** del Dockerfile con valor `/api` (mismo origen detrás del reverse proxy). |

Vite expone al cliente solo las variables con prefijo `VITE_`. El valor se
"hornea" en el build, por eso en Docker se pasa como argumento de construcción.

---

## Comunicación con el backend

- `services/api.js` crea la instancia de axios con:
  - `baseURL = import.meta.env.VITE_API_URL`
  - `withCredentials: true` (para enviar la cookie HttpOnly de refresh)
  - **Interceptores**: desenvuelven el `ApiResponse` del backend (devuelven
    `.data` real) y fuerzan logout cuando el refresh token expira.
- Las descargas (Excel/PDF) usan `responseType: 'blob'` mediante
  `services/downloadHelper.js`.

---

## Diseño y sistema visual

- Todo el estilo transversal está centralizado en **`src/theme.js`** (MUI theme),
  de modo que un cambio ahí afecta a todas las vistas de forma uniforme.
- Identidad de marca: **rosa DIF** como color primario, fondo blanco,
  tipografía oscura con acentos rosa.
- **Sidebar** blanco con el ítem activo resaltado en rosa; **sin barra superior
  (header)** en escritorio. En móvil, un botón hamburguesa flotante abre el menú.
- **Responsividad**: las vistas se adaptan a móvil. En particular, `DynamicTable`
  se muestra como tabla en escritorio y como **tarjetas apiladas en móvil**,
  evitando el scroll horizontal. Se logra con MUI + `useMediaQuery` (sin libs extra).

---

## Rutas

| Ruta | Acceso | Vista |
|---|---|---|
| `/login-admin` | Pública | Login con contraseña |
| `/identificacion` | Pública | Identificación rápida (kiosco) |
| `/asistencia` | USER | Cámara para checar entrada/salida |
| `/mis-asistencias` | USER | Historial y justificaciones propias |
| `/admin/dashboard` | ADMIN/SUPERADMIN | Registro de asistencias |
| `/admin/usuarios` | ADMIN/SUPERADMIN | Gestión de usuarios + carga masiva/exportación |
| `/admin/areas` | ADMIN/SUPERADMIN | Gestión de áreas |
| `/admin/horarios` | ADMIN/SUPERADMIN | Gestión de horarios |
| `/admin/justificaciones` | ADMIN/SUPERADMIN | Catálogo de justificaciones |

Las páginas se cargan con **lazy loading** para reducir el bundle inicial.

---

## Build de producción y Docker

El `Dockerfile` usa build multi-stage:

1. **Build**: `node:18-alpine` instala dependencias y ejecuta `npm run build`
   (recibe `VITE_API_URL` como `ARG`).
2. **Runtime**: `nginx:alpine` sirve el contenido estático de `dist/` con
   `nginx.conf` propio — Nginx corre **sin root** y escucha en el **puerto 8080**,
   con fallback de SPA a `index.html` y cabeceras de seguridad.

El despliegue completo (con el reverse proxy Nginx Proxy Manager) se documenta
en el README de la raíz.
