# config-server — Phase 6 (CLAUDE.md §7). Multi-stage build so the image is
# reproducible from source alone: the build stage brings its own Maven, so
# this works on a clean clone even though mvnw/.mvn/ aren't committed to git
# (see the root .gitignore — mvnw is a personal IDE convenience, not a
# build-time dependency here).
#
# This Dockerfile is intentionally near-identical across all 8 Spring
# services, varying only in the exposed port — same "repeat it, don't share
# it" reasoning as DECISIONS.md D-8 (no parent POM) and D-33 (no shared JAR).

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy the POM first and resolve dependencies into a layer that only
# invalidates when pom.xml changes, not on every source edit.
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

EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
