# Etapa de build
FROM maven:3.9.4-eclipse-temurin-17 as build
WORKDIR /app
COPY . .
RUN mvn -pl player-service -am clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine as runtime
WORKDIR /app

COPY --from=build /app/player-service/target/player-service-*.jar app.jar

ENV JAVA_OPTS="-XX:+UseSerialGC -XX:MaxRAMPercentage=30.0 -Xss512k -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
