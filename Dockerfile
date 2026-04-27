FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests
CMD ["java", "-jar", "target/BackEndCR-0.0.1-SNAPSHOT.jar", "--server.port=$PORT"]