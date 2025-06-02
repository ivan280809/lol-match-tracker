FROM maven:3.9.4-eclipse-temurin-17 as build
WORKDIR /app
COPY . .
RUN mvn -pl match-history-service -am clean package -DskipTests

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/match-history-service/target/match-history-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
