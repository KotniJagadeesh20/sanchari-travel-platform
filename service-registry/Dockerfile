# ── Build stage ───────────────────────────────────────────────────────────
# Build context must be the repo root (travel-platform/), not this folder,
# because this is a module in a multi-module Maven reactor and needs the
# parent pom.xml to resolve dependency versions. See docker-compose.yml.
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copy the parent pom first so dependency resolution can be cached as its
# own Docker layer — only re-runs when pom.xml files actually change.
COPY pom.xml .
COPY service-registry/pom.xml service-registry/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY bus-booking-service/pom.xml bus-booking-service/pom.xml
COPY ride-share-service/pom.xml ride-share-service/pom.xml
COPY travel-packages-service/pom.xml travel-packages-service/pom.xml
COPY hotel-service/pom.xml hotel-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml

# Copy full source for the one module we're actually building.
COPY service-registry/src service-registry/src

RUN mvn -pl service-registry -am package -DskipTests -q

# ── Runtime stage ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# wget is used by docker-compose's healthcheck; not guaranteed present on
# every eclipse-temurin alpine variant, so install it explicitly.
RUN apk add --no-cache wget

# Run as non-root for defense in depth.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /workspace/service-registry/target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
