FROM gradle:8.5-jdk17 AS builder
WORKDIR /build
COPY --chown=gradle:gradle . .
RUN gradle build --no-daemon

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar", "/app/app.jar"]