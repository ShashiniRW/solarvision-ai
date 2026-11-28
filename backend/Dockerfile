# syntax=docker/dockerfile:1

# ---------- Stage 1: build the jar ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# fix Windows line endings so Linux can run mvnw
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY src/ src/
# tests run in GitHub CI (they need Docker for Testcontainers)
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B package -DskipTests && cp target/*.jar app.jar

# ---------- Stage 2: run (Java runtime only) ----------
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring
COPY --from=build /workspace/app.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
