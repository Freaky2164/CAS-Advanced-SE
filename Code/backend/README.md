# Frauenhaus-Verwaltung – Backend

Spring Boot 3 / Java 17 / PostgreSQL 16. Neubau der alten Compucrash-Swing-Anwendung
(Adress-, Spenden- und Bußgeldverwaltung). Details zur Migration: ../MIGRATION.md.

## Start (lokal)

    docker compose up -d db
    DB_PASSWORD=frauenhaus APP_ADMIN_PASSWORD=<initiales Admin-Passwort> SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

Maven muss nicht installiert sein – der Maven Wrapper (`./mvnw`) lädt sich die
passende Version selbst. `docker-compose.override.yml` mappt den DB-Port 5432
für die lokale Entwicklung auf den Host (in Produktion weglassen).

Oder komplett in Containern: `docker compose up -d` (siehe docker-compose.yml; das
lokale `docker-compose.override.yml` aktiviert dabei automatisch `SPRING_PROFILES_ACTIVE=dev`).
Flyway legt das Schema beim ersten Start an; beim allerersten Start wird der
Benutzer `admin` angelegt (Passwort aus `APP_ADMIN_PASSWORD`, sonst geloggter Zufallswert).

### Testdaten

Mit aktivem Profil `dev` spielt Flyway zusätzlich `db/testdata/V5__testdaten.sql` ein
(realistische, frei erfundene Mitglieder/Spenden/Bußgelder/Gerichte für Demo & manuelle Tests).
Ohne `SPRING_PROFILES_ACTIVE=dev` (z. B. in Produktion) bleibt die Datenbank leer bis auf
die festen Stammwerte aus `V1__baseline_schema.sql` ('Frauenhaus'/'Förderverein').

Das Angular-Frontend liegt in `../frontend` (Start: `npm start`, siehe dortiges README).

## API (Auszug, HTTP Basic Auth)

    GET  /api/reports/bussgeld-uebersicht?von=2026-01-01&bis=2026-06-30
    GET  /api/reports/bussgeld-detail?von=...&bis=...&verein=Frauenhaus
    GET  /api/reports/bussgeld-bestaetigung/{bussgeldId}     → docx
    GET  /api/reports/spenden-uebersicht?jahr=2025
    GET  /api/reports/spendenquittung/{spendeId}
    GET  /api/reports/verteiler-emails?stichworte=a&stichworte=b
    GET  /api/reports/serienbrief-adressen?stichworte=...
    POST /api/stichworte/zusammenstellen  {"neu": "...", "alte": ["...", "..."]}
    POST /api/stichworte/zusammenfassen   {"neu": "...", "alte": ["..."]}

## Offene Punkte

- Datenübernahme aus der Alt-DB (Bestandsdaten per `pg_dump`/`COPY` in das neue Schema).
- CRUD-Endpoints für Mitglieder/Spenden/Bußgelder ergänzen, sobald das Frontend steht.
- Word-Vorlagen (`vorlagen-alt/`, .dot von Word 97) bei Bedarf nach .docx konvertieren;
  Briefe werden aktuell programmatisch als docx erzeugt.
- Backup: ops/BACKUP.md (pgBackRest, PITR, Restore-Tests).
