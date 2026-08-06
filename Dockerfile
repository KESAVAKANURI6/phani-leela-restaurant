# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Production stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV COGNODB_URI="bolt+s://db-c557d48d.databases.cognodb.com"
ENV COGNODB_USERNAME="cognodb"
ENV COGNODB_PASSWORD="0530d9e5153be3a70f7ca7de1ecb0e13"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
