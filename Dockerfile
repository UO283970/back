FROM gradle:8.5-jdk17 AS builder
WORKDIR /build
COPY --chown=gradle:gradle . .

FROM openjdk:17-jdk-alpine

# Creamos una carpeta para la app
WORKDIR /app

# Copiamos el jar construido desde el builder
COPY --from=builder /build/build/libs/*.jar app.jar

# Puerto expuesto
EXPOSE 8080

# Comando de arranque
ENTRYPOINT ["java", "-jar", "/app/app.jar"]