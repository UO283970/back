FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY build/libs/*.jar back-0.0.1-SNAPSHOT.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/back-0.0.1-SNAPSHOT.jar"]