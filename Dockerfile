FROM maven:3.8.6-jdk-11
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests
EXPOSE 8080
CMD ["mvn", "spring-boot:run"]