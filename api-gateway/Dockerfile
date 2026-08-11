# api-gateway — Phase 6. See config-server/Dockerfile for the full rationale
# (multi-stage build, no reliance on the uncommitted mvnw); this file only
# differs in the exposed port.

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring:spring

EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
