# Stage 1: build
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
# Кэшируем зависимости с принудительным обновлением
RUN mvn dependency:go-offline -B -U

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Создаем непривилегированного пользователя для безопасности
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Исправляем имя JAR файла - должно совпадать с вашим pom.xml
COPY --from=builder /app/target/yoga-telegram-bot-1.0.0.jar app.jar

EXPOSE 8080

# Health check (уберите если нет эндпоинта /health)
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Оптимизированные JVM параметры
CMD java -XX:+UseContainerSupport \
         -XX:MaxRAMPercentage=75.0 \
         -Xmx512m \
         -Xms256m \
         -jar app.jar