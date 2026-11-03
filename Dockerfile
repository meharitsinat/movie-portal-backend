FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY . .
RUN mvn -B -Dmaven.test.skip=true package \
    && mkdir -p /app \
    && cp auth-service/target/auth-service-1.0.0.jar /app/auth-service.jar \
    && cp movie-service/target/movie-service-1.0.0.jar /app/movie-service.jar \
    && cp order-service/target/order-service-1.0.0.jar /app/order-service.jar

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app /app
EXPOSE 10000
CMD ["java", "-jar", "/app/auth-service.jar"]
