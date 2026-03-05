# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY community-core/pom.xml community-core/
COPY community-gateway/pom.xml community-gateway/
COPY community-club/pom.xml community-club/
COPY community-user/pom.xml community-user/
COPY community-admin/pom.xml community-admin/
COPY community-activity/pom.xml community-activity/
COPY community-recruit/pom.xml community-recruit/
COPY community-notice/pom.xml community-notice/
# Download dependencies first (cache layer)
RUN mvn dependency:go-offline -q
COPY . .
RUN mvn clean package -DskipTests -q

# Stage 2: Run
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/community-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
