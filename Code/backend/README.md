# Frauenhaus-Verwaltung – Backend

Spring Boot / Java 25 / PostgreSQL 16. Neubau der alten Compucrash-Swing-Anwendung
(Adress-, Spenden- und Bußgeldverwaltung). Details zur Migration: ../MIGRATION.md.

## Start (komplett in Containern, mit Bestandsdaten)

    APP_ADMIN_PASSWORD=<initiales Admin-Passwort> docker compose up -d --build

Beim allerersten Start (leeres DB-Volume) laufen die Init-Skripte:

1. `V1__baseline_schema.sql` – Zielschema (`frauenhaus.*`, `app.app_user`)
2. `db/init/02_altdaten_vorbereitung.sql` – Import-Rolle für das Alt-Backup
3. `../data.sql` – das Backup des Altsystems (rebasedata-Dump, `public._frauenhaus_*`)
4. `db/init/04_datenuebernahme.sql` – Transformation ins Zielschema (Typkonvertierung,
   Leerwert-Bereinigung, Sequenzen) und Löschen aller Alt-Tabellen

Die Übernahme protokolliert die Zeilenzahlen ins DB-Log (`docker compose logs db`).
Neu aufsetzen: `docker compose down -v && docker compose up -d --build`.

Beim allerersten Anwendungsstart wird der Benutzer `admin` angelegt
(Passwort aus `APP_ADMIN_PASSWORD`, sonst geloggter Zufallswert).

## Start (Backend lokal, DB im Container)

    docker compose up -d db
    DB_PASSWORD=frauenhaus APP_ADMIN_PASSWORD=<initiales Admin-Passwort> ./mvnw spring-boot:run

Maven muss nicht installiert sein – der Maven Wrapper (`./mvnw`) lädt sich die
passende Version selbst. `docker-compose.override.yml` mappt den DB-Port 5432
für die lokale Entwicklung auf den Host (in Produktion weglassen).

Das Angular-Frontend liegt in `../frontend` (Start: `npm start`, siehe dortiges README).

## Dokumente aus Word-Vorlagen

Bußgeldbestätigungen und Spendenbescheinigungen werden serverseitig aus den
Word-Vorlagen in `vorlagen/` erzeugt (Lesezeichen-Felder, POI-HWPF, Ergebnis .doc):

- `FHBG.dot` / `FVBG.dot` – Zahlungsbestätigung an das Gericht je Träger
- `FHSB<Typ>.dot` / `FVSB<Typ>.dot` – Spendenbescheinigung je Träger und Spendentyp
  (`Geldspende`, `Dauerspende`, `Mitgliedsbeitrag`, `Sachspende`); fehlt die
  typspezifische Vorlage, wird die allgemeine `FHSB.dot`/`FVSB.dot` verwendet.

Das Vorlagen-Verzeichnis ist über `APP_VORLAGEN_PFAD` konfigurierbar
(Standard `vorlagen`, im Docker-Image `/app/vorlagen`).

## API (Auszug, HTTP Basic Auth)

    GET  /api/reports/bussgeld-uebersicht?von=2005-01-01&bis=2006-12-31
    GET  /api/reports/bussgeld-detail?von=...&bis=...&verein=Frauenhaus
    GET  /api/reports/bussgeld-bestaetigung/{bussgeldId}     → .doc aus FHBG/FVBG.dot
    GET  /api/reports/spenden-uebersicht?jahr=2005
    GET  /api/reports/spendenquittung/{spendeId}             → .doc aus FHSB*/FVSB*.dot
    GET  /api/reports/verteiler-emails?stichworte=a&stichworte=b
    GET  /api/reports/serienbrief-adressen?stichworte=...
    POST /api/stichworte/zusammenstellen  {"neu": "...", "alte": ["...", "..."]}
    POST /api/stichworte/zusammenfassen   {"neu": "...", "alte": ["..."]}

## Offene Punkte

- CRUD-Endpoints für Mitglieder/Spenden/Bußgelder ergänzen, sobald das Frontend steht.
- Serienbriefe: die `FHSerienBrief.dot`/`SpendenQuittung*.dot`-Vorlagen sind
  Word-Seriendruckdokumente (Datenquelle: xlsx von `/api/reports/serienbrief-adressen`).
- Backup: pgBackRest-Konzept (ops/) wieder vervollständigen, Restore-Tests.
