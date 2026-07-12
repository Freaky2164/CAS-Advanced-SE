# Build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Run (schlankes JRE, non-root)
FROM eclipse-temurin:25-jre
RUN useradd --system --no-create-home app
USER app
COPY --from=build /build/target/*.jar /app/app.jar
# Word-Vorlagen (.dot) für Bußgeldbestätigungen und Spendenbescheinigungen
COPY vorlagen /app/vorlagen
ENV APP_VORLAGEN_PFAD=/app/vorlagen
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
