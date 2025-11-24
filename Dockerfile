FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/yoga-telegram-bot-1.0.0.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]