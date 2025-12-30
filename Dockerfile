FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 9091

ENTRYPOINT ["java","-jar","app.jar"]
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 9091

ENTRYPOINT ["java","-jar","app.jar"]
