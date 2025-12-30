# ===== BUILD STAGE =====
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# ===== RUN STAGE =====
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 9091
ENTRYPOINT ["java","-jar","app.jar"]
