FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY service-registry/pom.xml service-registry/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY bus-booking-service/pom.xml bus-booking-service/pom.xml
COPY ride-share-service/pom.xml ride-share-service/pom.xml
COPY travel-packages-service/pom.xml travel-packages-service/pom.xml
COPY hotel-service/pom.xml hotel-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml

COPY travel-packages-service/src travel-packages-service/src

RUN mvn -pl travel-packages-service -am package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# wget is used by docker-compose's healthcheck.
RUN apk add --no-cache wget

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /workspace/travel-packages-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
