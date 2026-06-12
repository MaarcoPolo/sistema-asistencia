# Etapa 1: Construcción (Build)
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# URL del backend inyectada en tiempo de build. Por defecto "/api" para que el
# frontend llame al backend bajo el mismo origen detrás del reverse proxy.
# En desarrollo local sin proxy, pásalo como build-arg (ej. http://localhost:8080/api).
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# Etapa 2: Servidor Web Nginx (sin root, escuchando en 8080)
FROM nginx:alpine

# Config propia: escucha en 8080 (puertos <1024 requieren root) y añade
# cabeceras de seguridad para el contenido estático (ID-014/ID-016).
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copia la build de Vite (carpeta "dist").
COPY --from=build /app/dist /usr/share/nginx/html

# Crea un usuario no-root y le da permisos sobre los directorios que Nginx
# necesita escribir (cache, pid, logs). Luego corre como ese usuario.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && touch /var/run/nginx.pid \
    && chown -R appuser:appgroup /var/cache/nginx /var/run/nginx.pid /usr/share/nginx/html \
    && sed -i '/user  nginx;/d' /etc/nginx/nginx.conf

USER appuser

EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]