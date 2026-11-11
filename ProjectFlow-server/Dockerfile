# Build multi-estagio: compila com Maven (JDK 21) e executa em uma imagem JRE enxuta.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Camada separada para as dependencias (reaproveita cache quando so o codigo muda)
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /workspace/target/projectmaker-backend-1.0.0.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
