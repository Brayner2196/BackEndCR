# Etapa 1: build con Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: runtime liviano
FROM eclipse-temurin:21-jdk
WORKDIR /app
# El backend opera en UTC de forma explicita e independiente del host.
ENV TZ=UTC
COPY --from=build /app/target/*.jar app.jar

CMD ["java", "-Duser.timezone=UTC", "-jar", "app.jar", "--server.port=8080"]