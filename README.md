# Sistema de Asistencia 

Una aplicación web full-stack para el registro y gestión de asistencias de personal, desarrollada con Spring Boot para el backend y React para el frontend.

El sistema permite a los usuarios registrar su entrada y salida mediante captura de webcam, mientras que los administradores pueden gestionar usuarios, áreas, horarios y generar reportes detallados en formato Excel y PDF.

## ✨ Características Principales

* **Gestión de Entidades:** CRUD completo para Usuarios, Áreas y Horarios.
* **Control de Acceso Basado en Roles (RBAC):** Roles predefinidos (SUPERADMIN, ADMIN, USER) con permisos específicos en toda la aplicación.
* **Autenticación Segura:** Sistema de autenticación mediante JSON Web Tokens (JWT).
* **Registro de Asistencia con Webcam:** Captura de foto al momento de registrar la entrada y salida.
* **Lógica de Negocio:** Detección automática de retardos basada en horarios configurables (globales, por área o por usuario).
* **Dashboard de Administración:** Visualización, filtrado, búsqueda y paginación de todos los registros de asistencia.
* **Reportes Avanzados:** Generación de reportes dinámicos en formato **.xlsx (Excel)** y **.pdf**, con múltiples opciones de filtrado.
* **Flujo de Kiosco:** Cierre de sesión automático después de cada registro, ideal para dispositivos compartidos.

## 🛠️ Stack Tecnológico

#### ⚙️ Backend
* **Java 17**
* **Spring Boot 3**
* **Spring Security** (con JWT)
* **Spring Data JPA** (Hibernate)
* **Maven**
* **PostgreSQL Driver**
* **Apache POI** (para reportes Excel)
* **OpenPDF** (para reportes PDF)

#### 💻 Frontend
* **React 18**
* **Vite** como empaquetador
* **Material-UI (MUI)** para los componentes de la interfaz.
* **Axios** para las peticiones a la API.
* **React Router DOM** para el enrutamiento.

#### 🗄️ Base de Datos
* **PostgreSQL**

---
## 🚀 Instalación y Puesta en Marcha

Sigue estos pasos para levantar el proyecto en un entorno de desarrollo local.

### Prerrequisitos
Asegúrate de tener instalado el siguiente software:
* Java JDK 17 o superior.
* Apache Maven 3.8 o superior.
* Node.js v18 o superior (incluye npm).
* Una instancia de PostgreSQL en ejecución.

### 1. Clonar el Repositorio
git clone [URL_DE_TU_REPOSITORIO]
cd [NOMBRE_DE_LA_CARPETA_DEL_PROYECTO]
# ================================================================
# SCRIPT DE INSTALACIÓN RÁPIDA PARA EL SISTEMA DE ASISTENCIA
# ================================================================
#
# Este script configurará el backend y el frontend.
# Asegúrate de tener PostgreSQL, Java 17, Maven y Node.js instalados.
#

# --- PASO 1: Configuración de la Base de Datos ---
echo "--- PASO 1: Configuración de la Base de Datos ---"
echo "Por favor, abre tu cliente de PostgreSQL (como pgAdmin) y ejecuta el siguiente comando:"
echo "
CREATE DATABASE asistencia_db;
"
echo "✅ Presiona Enter cuando hayas creado la base de datos para continuar..."
read -p ""


# --- PASO 2: Configuración del Backend ---
echo "--- PASO 2: Configurando el Backend ---"
cd backend

echo "Creando archivo application.properties... (¡NO OLVIDES EDITARLO LUEGO!)"
cat <<EOF > src/main/resources/application.properties
# ================================================
# CONFIGURACIÓN DE LA APLICACIÓN DE ASISTENCIA
# ================================================

# Configuración de la Base de Datos PostgreSQL
# POR FAVOR, REEMPLAZA CON TUS DATOS REALES
spring.datasource.url=jdbc:postgresql://localhost:5432/asistencia_db
spring.datasource.username=postgres
spring.datasource.password=tu_contraseña_de_postgres

# Configuración de JPA/Hibernate (creará las tablas automáticamente)
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Configuración de JWT
# CAMBIA ESTE SECRETO POR UNO PROPIO, LARGO Y SEGURO
app.jwt.secret=ESTE_ES_UN_SECRETO_MUY_LARGO_Y_SEGURO_PARA_JWT_CAMBIAME
app.jwt.expiration-ms=86400000 # 24 horas
EOF

echo "Compilando el proyecto de backend con Maven (esto puede tardar un momento)..."
mvn clean install


# --- PASO 3: Configuración del Frontend ---
echo "--- PASO 3: Configurando el Frontend ---"
cd ../frontend

echo "Creando archivo .env..."
cat <<EOF > .env
VITE_API_BASE_URL=http://localhost:8080/api
EOF

echo "Instalando dependencias del frontend con npm (esto puede tardar un momento)..."
npm install


# --- ¡INSTALACIÓN COMPLETADA! ---
echo ""
echo "✅ ¡La configuración ha terminado!"
echo ""
echo "--- PRÓXIMOS PASOS ---"
echo "1. ❗️ IMPORTANTE: Abre el archivo 'backend/src/main/resources/application.properties' y edita tu usuario y contraseña de la base de datos."
echo "2. Abre DOS terminales en la carpeta del proyecto."
echo "3. En la Terminal 1, ejecuta el backend: cd backend && mvn spring-boot:run"
echo "4. En la Terminal 2, ejecuta el frontend: cd frontend && npm run dev"
echo ""

# Regresar a la carpeta raíz
cd ..
