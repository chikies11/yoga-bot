# Build stage
FROM maven:3.8.6-eclipse-temurin-11 AS build
WORKDIR /workspace/app
COPY pom.xml .
COPY src src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:11-jre-focal
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
WORKDIR /app
COPY --from=build /workspace/app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]