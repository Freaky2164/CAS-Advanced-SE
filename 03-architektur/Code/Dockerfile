# Build
# Bewusst das offizielle JDK-Image + der im Repo liegende Maven Wrapper statt eines
# maven:<version>-eclipse-temurin-<jdk>-Images: für die im Wrapper festgelegte
# Maven-Version (3.9.9) existiert kein Kombi-Tag mit JDK 25 auf Docker Hub. So ist
# garantiert, dass lokaler Build (./mvnw) und Image-Build dasselbe Maven verwenden.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
# Wrapper zuerst kopieren, damit der Maven-Download in einer eigenen Cache-Schicht landet
COPY mvnw .
COPY .mvn ./.mvn
COPY pom.xml .
# "sh ./mvnw" statt "./mvnw": auf Windows-Checkouts fehlt dem Skript das Executable-Bit
RUN sh ./mvnw -q -B dependency:go-offline
COPY src ./src
# -Pproduction baut das Vaadin-Frontend als optimiertes Produktions-Bundle
# (das vaadin-maven-plugin lädt sich Node.js dafür selbst herunter)
RUN sh ./mvnw -q -B -DskipTests -Pproduction package

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