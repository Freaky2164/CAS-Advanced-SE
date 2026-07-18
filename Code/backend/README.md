# Frauenhaus-Verwaltung – Backend + Vaadin-UI

Spring Boot / Java 25 / Vaadin 25 / PostgreSQL 18. Neubau der alten
Compucrash-Swing-Anwendung (Adress-, Spenden- und Bußgeldverwaltung).
Details zur Migration: ../MIGRATION.md.

Das UI ist seit dem Vaadin-Umbau (Branch `vaadin-prototype`) ein server-seitiges
Java-UI (Vaadin Flow, Paket `de.frauenhaus.ui`) und wird direkt vom Backend
ausgeliefert – das frühere Angular/npm-Frontend und der nginx-Container entfallen.
Die Views rufen die Service-Schicht direkt auf; die REST-API unter `/api` bleibt
unverändert bestehen (HTTP Basic, z.B. für Skripte und Tests).

## Start (komplett in Containern, mit Bestandsdaten)

    APP_ADMIN_PASSWORD=<initiales Admin-Passwort> docker compose up -d --build

Danach läuft die komplette Anwendung unter **http://localhost:8080** (Port über
`WEB_PORT` änderbar) – Anmeldung über die Login-Seite mit den `app.app_user`-Benutzern.
Die DB bleibt vom Host aus unerreichbar; `docker-compose.override.yml` mappt sie
nur für die lokale Entwicklung auf 127.0.0.1:15432. Produktion startet ohne sie:

    docker compose -f docker-compose.yml up -d --build

**TLS:** terminiert in Produktion ein vorgelagerter Reverse-Proxy (nginx, caddy,
Traefik …) vor `backend:8080`; der Prototyp selbst spricht HTTP.

Beim allerersten Start (leeres DB-Volume) laufen die Init-Skripte:

1. `V1__baseline_schema.sql` – Zielschema (`frauenhaus.*`, `app.app_user`, Envers-Audit)
2. `db/init/02_altdaten_vorbereitung.sql` – Import-Rolle für das Alt-Backup
3. `../data.sql` – das Backup des Altsystems (rebasedata-Dump, `public._frauenhaus_*`)
4. `db/init/04_datenuebernahme.sql` – Transformation ins Zielschema (Typkonvertierung,
   Leerwert-Bereinigung, Sequenzen) und Löschen aller Alt-Tabellen
5. `db/init/05_sicherheit.sh` – App-Rolle `frauenhaus_backend` + Row Level Security

Die Übernahme protokolliert die Zeilenzahlen ins DB-Log (`docker compose logs db`).
Neu aufsetzen: `docker compose down -v && docker compose up -d --build`.

## Datensicherheit (DSGVO)

Die Anwendung verbindet sich nicht mehr als Schema-Eigentümer, sondern als
eingeschränkte Rolle `frauenhaus_backend` (kein DDL, kein SUPERUSER, kein
BYPASSRLS; Passwort über `DB_APP_PASSWORD`). Auf allen Tabellen mit
personenbezogenen Daten (Mitglieder, Spenden, Bußgelder, Zahlungseingänge,
Stichwort-/Vereinszuordnungen, Dokument-Anhänge, Änderungshistorie) ist
**Row Level Security** aktiv: Zeilen sind nur sichtbar und änderbar, wenn die
Verbindung den authentifizierten Benutzerkontext trägt. Den setzt
`RowLevelSecurityDataSource` bei jeder Connection-Ausleihe aus dem angemeldeten
Spring-Security-Benutzer (Session-Variablen `app.benutzer`/`app.benutzer_rolle`).
Wer sich mit den Backend-Zugangsdaten direkt per psql verbindet, sieht **keine
einzige Zeile** – und kann auch keine einfügen. `app.app_user` bleibt bewusst
ohne RLS (wird bei der Anmeldung gelesen, bevor ein Benutzerkontext existiert).

Beim allerersten Anwendungsstart wird der Benutzer `admin` angelegt
(Passwort aus `APP_ADMIN_PASSWORD`, sonst geloggter Zufallswert).

## Start (Backend lokal, DB im Container)

    docker compose up -d db
    DB_PORT=15432 DB_PASSWORD=frauenhaus APP_ADMIN_PASSWORD=<initiales Admin-Passwort> ./mvnw spring-boot:run

Maven muss nicht installiert sein – der Maven Wrapper (`./mvnw`) lädt sich die
passende Version selbst. `docker-compose.override.yml` mappt den DB-Port für die
lokale Entwicklung auf Host-Port **15432** (5432 ist auf Entwickler-Rechnern oft
von einem lokalen Postgres belegt). Tests analog: `DB_PORT=15432 mvn test`.

UI und API laufen dann gemeinsam unter http://localhost:8080. Im Entwicklungsmodus
nutzt Vaadin sein vorkompiliertes Dev-Bundle – npm/Node ist lokal nicht nötig,
solange nur Standard-Komponenten verwendet werden. Erst der Produktions-Build
(`./mvnw package -Pproduction`, macht das Dockerfile) baut das optimierte
Frontend-Bundle und lädt sich Node.js dafür bei Bedarf selbst herunter.

## Dokumente aus Word-Vorlagen

Bußgeldbestätigungen und Spendenbescheinigungen werden serverseitig aus den
Word-Vorlagen in `vorlagen/` erzeugt (Lesezeichen-Felder, POI-HWPF, Ergebnis .doc):

- `FHBG.dot` / `FVBG.dot` – Zahlungsbestätigung an das Gericht je Träger
- `FHSB<Typ>.dot` / `FVSB<Typ>.dot` – Spendenbescheinigung je Träger und Spendentyp
  (`Geldspende`, `Dauerspende`, `Mitgliedsbeitrag`, `Sachspende`); fehlt die
  typspezifische Vorlage, wird die allgemeine `FHSB.dot`/`FVSB.dot` verwendet.

Mehrzeilige Werte (Zahlungslisten, Einzelspenden) übernehmen die
Absatzformatierung des Lesezeichen-Absatzes – Aufzählungen (Bulletpoints/
Nummerierungen) werden also für jede Zeile weitergeführt.

Das Vorlagen-Verzeichnis ist über `APP_VORLAGEN_PFAD` konfigurierbar
(Standard `vorlagen`, im Docker-Image `/app/vorlagen`).

## API (Auszug, HTTP Basic Auth)

    GET  /api/reports/bussgeld-uebersicht?von=2005-01-01&bis=2006-12-31
    GET  /api/reports/bussgeld-detail?von=...&bis=...&verein=Frauenhaus
    GET  /api/reports/bussgeld-bestaetigung/{bussgeldId}     → .doc aus FHBG/FVBG.dot
    GET  /api/reports/spenden-uebersicht?jahr=2005
    GET  /api/reports/spendenquittung/{spendeId}             → .doc aus FHSB*/FVSB*.dot
    GET  /api/reports/verteiler-emails?stichworte=a&stichworte=b
    POST /api/reports/verteiler/versenden                    → Sammel-E-Mail per BCC (SMTP nötig)
    GET  /api/reports/serienbrief-adressen?stichworte=...
    GET  /api/reports/serienbrief?stichworte=...&verein=Frauenhaus&text=...   (text optional)
    GET  /api/reports/stichwortsuche?stichworte=...          (auch als .xlsx)
    POST /api/stichworte/zusammenstellen  {"neu": "...", "alte": ["...", "..."]}
    POST /api/stichworte/zusammenfassen   {"neu": "...", "alte": ["..."]}

Stammdaten-CRUD (Seiten über `page`/`size`/`sort`, Volltextfilter über `suche`):
`/api/mitglieder`, `/api/spenden`, `/api/bussgelder` (inkl. `/eingaenge`),
`/api/gerichte`, `/api/vereine`, `/api/spendenarten`, `/api/spendentypen`,
`/api/anreden`, `/api/bussgeldstatus`. Änderungsverlauf je Datensatz unter
`.../{id}/verlauf` (Hibernate Envers), Datei-Anhänge unter `/api/dokumente/...`,
Benutzerverwaltung (nur ADMIN) unter `/api/admin/users`.

## Offene Punkte

- Serienbriefe: die `FHSerienBrief.dot`/`SpendenQuittung*.dot`-Vorlagen sind
  Word-Seriendruckdokumente (Datenquelle: xlsx von `/api/reports/serienbrief-adressen`).
- Verteiler-Versand braucht einen erreichbaren SMTP-Server (`MAIL_HOST`, `MAIL_PORT`,
  `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_ABSENDER`) – ohne ihn funktioniert alles außer
  `/api/reports/verteiler/versenden`.
- Backup: pgBackRest-Konzept (ops/) wieder vervollständigen, Restore-Tests.
