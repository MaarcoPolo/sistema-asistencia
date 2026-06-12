# Etapa 1: Construcción (Build)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Descarga las dependencias para usar el caché de Docker
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Run)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuario no-root para reducir el impacto si el contenedor se ve comprometido (ID-016).
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8080
# JAVA_OPTS permite ajustar memoria/GC desde docker-compose (PERF-007).
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]