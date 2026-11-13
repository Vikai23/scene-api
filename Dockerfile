# Etapa 1: compila o projeto com Maven e JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

# Etapa 2: roda só o .jar com o JRE 21 (o plano grátis do Render tem 512 MB de memória)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-jar", "app.jar"]
