# Универсальный образ для сервисов gossips.
# Сборка: docker compose (аргумент MODULE = имя модуля maven).
# Мультистейдж: сначала сборка jar в maven-образе, потом минимальный JRE-рантайм.
ARG MODULE

FROM maven:3.9-eclipse-temurin-21-alpine AS build
ARG MODULE
WORKDIR /build
COPY . .
# Собираем только нужный модуль и его зависимости (common).
# Тесты пропускаем — они гоняются в CI, здесь только упаковка.
RUN mvn -B -q -pl ${MODULE} -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
ARG MODULE
WORKDIR /app
RUN addgroup -S gossips && adduser -S gossips -G gossips
COPY --from=build /build/${MODULE}/target/*.jar app.jar
USER gossips
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
