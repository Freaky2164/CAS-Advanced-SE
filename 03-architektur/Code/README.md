# Frauenhaus-Verwaltung

Neubau der alten Compucrash-Swing-Anwendung (Adress-, Spenden- und Bußgeldverwaltung)
als Spring-Boot-Anwendung mit server-seitigem Vaadin-UI.

**Stack:** Java 25 · Spring Boot · Vaadin 25 (Flow, Paket `de.frauenhaus.ui`) · PostgreSQL 18 · Flyway
**Migrationsdetails:** [`../MIGRATION.md`](../MIGRATION.md)

UI und REST-API laufen gemeinsam im Backend unter **http://localhost:8080**. Die Views rufen die Service-Schicht direkt
auf, die REST-API unter `/api` (HTTP Basic) bleibt für Skripte und Tests bestehen.

---

## Schnellstart

### Voraussetzungen

| | |
|---|---|
| **Docker** (mit Compose) | für die Datenbank – zwingend |
| **JDK 25** | nur für Weg A (lokaler Backend-Start). Maven **nicht** nötig: `./mvnw` lädt sich Maven 3.9.9 selbst |
| `../data.sql` | nur für Weg B – das Backup des Altsystems, eine Verzeichnisebene **über** diesem Projekt |
| `vorlagen/*.dot` | optional – nur für Word-Bescheinigungen ([Details](#dokumente-aus-word-vorlagen)) |

### Weg A – leere Datenbank (empfohlen zum Entwickeln und Ausprobieren)

Schema, Härtung und realistische Testdaten legt Flyway beim App-Start selbst an.

```bash
# 1. Nur die Datenbank starten
docker compose up -d db

# 2. Backend starten (legt Schema + Testdaten an)
DB_PORT=15432 DB_PASSWORD=frauenhaus APP_ADMIN_PASSWORD=<admin-passwort> ./mvnw spring-boot:run
```

Windows (cmd):

```cmd
docker compose up -d db
set DB_PORT=15432 && set DB_PASSWORD=frauenhaus && set APP_ADMIN_PASSWORD=<admin-passwort> && mvnw spring-boot:run
```

Fertig: **http://localhost:8080**, Anmeldung mit `admin` und dem gesetzten Passwort.

> Solange `../data.sql` fehlt, in `docker-compose.yml` die Mounts der initdb-Skripte
> **02–04** auskommentieren. Unter Windows/macOS scheitert der Start dann sichtbar am
> Bind-Mount; **unter Linux legt Docker die fehlende Datei stillschweigend als leeres
> Verzeichnis an** – Postgres bleibt in der Initialisierung hängen und nimmt nie
> Verbindungen an (`docker compose logs db` zeigt es). Der DB-Port liegt lokal auf
> **15432** (`docker-compose.override.yml`), weil 5432 auf Entwickler-Rechnern oft belegt ist.

### Weg B – alles in Containern, mit Altdaten

```bash
APP_ADMIN_PASSWORD=<admin-passwort> docker compose up -d --build
```

Der erste Start importiert `../data.sql` und übernimmt die Altdaten ins Zielschema
(Ablauf: [Erstinitialisierung der Datenbank](#erstinitialisierung-der-datenbank)).
Anschließend läuft alles unter **http://localhost:8080** (Port über `WEB_PORT`).

Produktion – ohne die Entwicklungs-Overrides (kein Host-Port auf der DB, kein Mailpit,
kein `dev`-Profil):

```bash
docker compose -f docker-compose.yml up -d --build
```

TLS terminiert in Produktion ein vorgelagerter Reverse-Proxy (nginx, caddy, Traefik …)
vor `backend:8080`; die Anwendung selbst spricht HTTP.

### Neu aufsetzen / Reset

```bash
docker compose down -v && docker compose up -d --build   # löscht das DB-Volume
```

### Der Admin-Benutzer

Beim allerersten Anwendungsstart wird `admin` angelegt – Passwort aus `APP_ADMIN_PASSWORD`,
sonst ein zufälliges, ins Log geschriebenes. **Das greift nur, solange `app.app_user` leer
ist**; auf einer bestehenden Datenbank wird `APP_ADMIN_PASSWORD` ignoriert (Passwort dann
über die Benutzerverwaltung zurücksetzen oder Volume löschen).

---

## Konfiguration (Umgebungsvariablen)

| Variable | Default | Bedeutung |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `frauenhaus` | Datenbankverbindung (lokal: `DB_PORT=15432`) |
| `DB_USER` / `DB_PASSWORD` | `frauenhaus_backend` / – | Anwendungsrolle (eingeschränkt, RLS-pflichtig) |
| `DB_APP_PASSWORD` | `frauenhaus` (Compose) | Passwort, mit dem die Rolle `frauenhaus_backend` angelegt wird |
| `FLYWAY_DB_USER` / `FLYWAY_DB_PASSWORD` | `DB_USER` / `DB_PASSWORD` | Migrationen (brauchen DDL; im Stack `frauenhaus_app`) |
| `APP_ADMIN_PASSWORD` | – | initiales Passwort des Benutzers `admin` |
| `WEB_PORT` | `8080` | Host-Port des Backends |
| `APP_VORLAGEN_PFAD` | `vorlagen` (Image: `/app/vorlagen`) | Verzeichnis der Word-Vorlagen |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USER` / `MAIL_PASSWORD` / `MAIL_ABSENDER` | Dev: Mailpit | SMTP für den Verteiler-Versand |

---

## Projektstruktur

```
.
├── src/main/java/de/frauenhaus/     ← Anwendung (Services, REST-API, Vaadin-Views in .ui)
├── src/main/resources/
│   ├── application.yml, application-dev.yml
│   ├── frontend/                    ← Vaadin-Frontend-Quellen (Build-Eingabe, nicht im Artefakt)
│   └── db/
│       ├── migration/               ← Flyway, versioniert (V1, V6, V7 …) – läuft beim App-Start
│       ├── testdata/                ← Flyway, nur Profil dev (V5)
│       └── init/                    ← docker-initdb (02, 04, 05) – nur vom Compose-Stack gemountet
├── ops/                             ← Betriebs-/Werkzeug-Skripte (verify-setup, sonar, pgbackrest)
├── vorlagen/                        ← Word-Vorlagen (.dot, nicht im Repository)
├── docker-compose.yml               ← DB + Backend
├── docker-compose.override.yml      ← nur lokale Entwicklung (DB-Port 15432, dev-Profil, Mailpit)
└── docker-compose.sonarqube.yml     ← separater SonarQube-Stack
```

Zwei Verzeichnisse weichen bewusst vom Standard ab:

- **`src/main/resources/frontend`** statt `src/main/frontend`, damit alle Nicht-Java-Quellen an
  einer Stelle liegen (Property `vaadin.frontend.directory` + Parameter `frontendDirectory` des
  `vaadin-maven-plugin`; der Dev-Mode liest den Pfad aus `flow-build-info.json`). Es ist
  Build-Eingabe und per `<resources>`-`exclude` aus dem Artefakt ausgeschlossen – dort landet nur
  das fertige Bundle unter `META-INF/VAADIN`.
- **`src/main/resources/db/init`** liegt bei den übrigen DB-Skripten, obwohl es nie über den
  Classpath gelesen wird; ebenfalls per `exclude` aus dem Artefakt entfernt.

---

## Datenbank

### Erstinitialisierung der Datenbank

Nur beim allerersten Start auf leerem Volume (Weg B) arbeitet der Postgres-Entrypoint der Reihe
nach ab:

| Slot | Datei | Zweck |
|---|---|---|
| 01 | `db/migration/V1__baseline_schema.sql` | Zielschema (`frauenhaus.*`, `app.app_user`, Envers-Audit) |
| 02 | `db/init/02_altdaten_vorbereitung.sql` | Import-Rolle für das Alt-Backup |
| 03 | `../data.sql` | Backup des Altsystems (rebasedata-Dump, `public._frauenhaus_*`) |
| 04 | `db/init/04_datenuebernahme.sql` | Transformation ins Zielschema, danach Löschen der Alt-Tabellen |
| 05 | `db/init/05_sicherheit.sh` | legt **nur** die Login-Rolle `frauenhaus_backend` an |

Die Slots **01** und **03** haben keine Datei in `db/init/`: 01 kommt aus der Flyway-Migration
(das Schema wird nicht doppelt gepflegt), 03 ist das nicht eingecheckte Backup. Die Dateinamen
tragen dieselbe Nummer wie ihr Slot, damit die Reihenfolge lesbar bleibt – Details:
[`db/init/README.md`](src/main/resources/db/init/README.md).

Bei der Übernahme werden Spenden **ohne** Datum/Träger nicht verworfen (Aufbewahrungsfristen),
sondern mit gekennzeichneten Platzhaltern übernommen (Datum `1900-01-01`, Träger `unbekannt`,
Vermerk in der Bemerkung). Die Zeilenzahlen stehen im DB-Log (`docker compose logs db`).

### Schema-Migrationen (Flyway)

Verbindliche Regeln für neue Migrationen:
[`db/migration/README.md`](src/main/resources/db/migration/README.md).

| Version | Inhalt | Läuft auf |
|---|---|---|
| `V1` | Baseline-Schema (**eingefroren**, zusätzlich als initdb-Skript gemountet) | leere DB |
| `V2`–`V4` | bewusst frei – reserviert für die initdb-Schritte 02–04 | – |
| `V5` | Testdaten (`db/testdata`, eigene `locations`) | leere DB, nur Profil `dev` |
| `V6` | Audit-Historie append-only | alle |
| `V7` | App-Rolle, Rechtevergabe, Row Level Security (idempotent) | alle |

- **Leere Datenbank:** Flyway legt beim App-Start alles an (`V1`), härtet es (`V6`, `V7`) und
  spielt im Profil `dev` Testdaten ein (`V5`).
- **Bestands-Datenbank:** wird beim ersten Start bei Version 5 baselined – `V1`–`V5` gelten als
  angewendet, neue Migrationen (ab `V6`) laufen normal. Deshalb liegen die Testdaten auf `V5`:
  unterhalb der Baseline werden sie auf Altdaten-Beständen nie eingespielt (lägen sie z.B. auf
  `V8`, landeten sie in jeder Produktions-Datenbank). Aus demselben Grund sind `V2`–`V4` frei –
  die initdb-Schritte gibt es nur auf Bestands-Datenbanken und nie als Migration.
- Migrationen laufen mit eigenen Zugangsdaten (`FLYWAY_DB_*`), weil die App-Rolle bewusst kein
  DDL darf; im Compose-Stack ist das der Schema-Eigentümer `frauenhaus_app`.
- **`V1` niemals nachträglich ändern** (Checksum-Fehler, stille Divergenz auf Bestands-Datenbanken)
  – Änderungen ausschließlich als neue Migration ab `V8`.

---

## Datensicherheit (DSGVO)

Die Anwendung verbindet sich als eingeschränkte Rolle `frauenhaus_backend` (kein DDL, kein
SUPERUSER, kein BYPASSRLS; Passwort über `DB_APP_PASSWORD`) – nicht als Schema-Eigentümer.

Auf allen Tabellen mit personenbezogenen Daten (Mitglieder, Spenden, Bußgelder,
Zahlungseingänge, Stichwort-/Vereinszuordnungen, Dokument-Anhänge, Änderungshistorie) ist
**Row Level Security** aktiv: Zeilen sind nur sichtbar und änderbar, wenn die Verbindung den
authentifizierten Benutzerkontext trägt. Den setzt `RowLevelSecurityDataSource` bei jeder
Connection-Ausleihe aus dem angemeldeten Spring-Security-Benutzer (Session-Variablen
`app.benutzer` / `app.benutzer_rolle`). `app.app_user` bleibt bewusst ohne RLS – die Tabelle
wird bei der Anmeldung gelesen, bevor ein Benutzerkontext existiert.

**Grenzen von RLS in diesem Aufbau** (Erkenntnis aus dem Gruppen-Review): Die Session-Variablen
kann jede Verbindung per `set_config` selbst setzen – wer die Backend-Zugangsdaten besitzt, kann
sich den Kontext (inklusive Rolle `ADMIN`) also selbst geben. RLS schützt hier vor *kontextlosem*
Zugriff (ein direkt angebundenes psql oder ein naiver Client sieht ohne `set_config` nichts), ist
aber **keine harte Barriere gegen geleakte Zugangsdaten**. Die eigentlichen Schutzschichten sind:

- DB ohne veröffentlichten Port (nur im Compose-Netz),
- Zugangsdaten ausschließlich über Umgebungsvariablen,
- **append-only Audit-Historie**: `frauenhaus_backend` hat auf `*_aud` / `app.revinfo` kein
  UPDATE/DELETE (`V6`), Manipulationen lassen sich also nicht spurlos verstecken; der Zugriff auf
  die Flyway-Historie ist ebenfalls entzogen.

**Einzige Quelle des Sicherheitsmodells** ist die Migration `V7__sicherheit_rollen_und_rls.sql`
(Rechtevergabe, append-only, RLS-Policies) – idempotent, damit sie auf leeren wie auf
Bestands-Datenbanken läuft. Früher stand das Modell nur im initdb-Skript `05_sicherheit.sh`,
wodurch Datenbanken ohne diesen Pfad (lokale Entwicklung, CI, Tests) ganz ohne RLS blieben. Das
Skript legt heute nur noch die Login-Rolle an; das Passwort kommt aus `DB_APP_PASSWORD` und steht
damit nirgends im Repository (in `V7` als Flyway-Placeholder `app_db_password`). Ist die Variable
leer, werden weder Rolle noch RLS angelegt und `V7` protokolliert das.

---

## Dokumente aus Word-Vorlagen

Bußgeldbestätigungen und Spendenbescheinigungen entstehen serverseitig aus den Vorlagen in
`vorlagen/` (Lesezeichen-Felder, POI-HWPF, Ergebnis `.doc`):

- `FHBG.dot` / `FVBG.dot` – Zahlungsbestätigung an das Gericht je Träger
- `FHSB<Typ>.dot` / `FVSB<Typ>.dot` – Spendenbescheinigung je Träger und Spendentyp
  (`Geldspende`, `Dauerspende`, `Mitgliedsbeitrag`, `Sachspende`); fehlt die typspezifische
  Vorlage, greift die allgemeine `FHSB.dot` / `FVSB.dot`.

> **⚠️ Die `.dot`-Dateien liegen NICHT im Repository.** Sie sind fachliche Dokumentvorlagen des
> Vereins und werden von der **Vorlagen-Gruppe** gepflegt. Vor dem ersten Start in das Verzeichnis
> `vorlagen/` kopieren (im Docker-Image über `COPY vorlagen /app/vorlagen`).
>
> Ohne die Vorlagen startet die Anwendung normal; nur
> `GET /api/reports/bussgeld-bestaetigung/{id}` und `GET /api/reports/spendenquittung/{id}`
> (bzw. die entsprechenden Schaltflächen im UI) quittieren mit
> `IllegalStateException: Vorlage … nicht gefunden – app.vorlagen.pfad prüfen`. Stammdatenpflege,
> xlsx-Reports, Serienbriefe und Verteiler-Versand sind nicht betroffen – sie erzeugen ihre
> Dokumente programmatisch mit POI.

Mehrzeilige Werte (Zahlungslisten, Einzelspenden) übernehmen die Absatzformatierung des
Lesezeichen-Absatzes; Aufzählungen und Nummerierungen laufen also über alle Zeilen weiter.

---

## REST-API (Auszug, HTTP Basic Auth)

```
GET  /api/reports/bussgeld-uebersicht?von=2005-01-01&bis=2006-12-31
GET  /api/reports/bussgeld-detail?von=...&bis=...&verein=Frauenhaus
GET  /api/reports/bussgeld-bestaetigung/{bussgeldId}     → .doc aus FHBG/FVBG.dot
GET  /api/reports/spenden-uebersicht?jahr=2005
GET  /api/reports/spendenquittung/{spendeId}             → .doc aus FHSB*/FVSB*.dot
GET  /api/reports/verteiler-emails?stichworte=a&stichworte=b
POST /api/reports/verteiler/versenden                    → Sammel-E-Mail per BCC (SMTP nötig)
GET  /api/reports/serienbrief-adressen?stichworte=...
GET  /api/reports/serienbrief?stichworte=...&verein=Frauenhaus&text=...   (text optional)
GET  /api/reports/stichwortsuche?stichworte=...&foerderverein=&frauenhaus=
GET  /api/reports/stichwortsuche.xlsx?stichworte=...     (gleiche Filter, als Excel)
POST /api/stichworte/zusammenstellen  {"neu": "...", "alte": ["...", "..."]}
POST /api/stichworte/zusammenfassen   {"neu": "...", "alte": ["..."]}
GET  /api/me                                             → Benutzername + Rollen
```

Stammdaten-CRUD (Seiten über `page` / `size` / `sort`, Volltextfilter über `suche`):
`/api/mitglieder`, `/api/spenden`, `/api/bussgelder` (inkl. `/eingaenge`), `/api/gerichte`,
`/api/vereine`, `/api/spendenarten`, `/api/spendentypen`, `/api/anreden`, `/api/bussgeldstatus`.
Änderungsverlauf je Datensatz unter `.../{id}/verlauf` (Hibernate Envers), Datei-Anhänge unter
`/api/dokumente/...` (max. 10 MB je Datei, `spring.servlet.multipart.max-file-size`),
Benutzerverwaltung (nur ADMIN) unter `/api/admin/users`.

---

## Tests

**Die Integrationstests brauchen eine erreichbare Datenbank** – ohne sie schlägt der
Spring-Kontext fehl und alle `@SpringBootTest`-Klassen laufen auf Fehler
(`FlywaySqlUnableToConnectToDbException: Unable to obtain connection from database`):

```bash
docker compose up -d db
DB_PORT=15432 DB_PASSWORD=frauenhaus ./mvnw verify   # Tests + JaCoCo-Report unter target/site/jacoco/index.html
```

Windows (cmd):

```cmd
docker compose up -d db
set DB_PORT=15432 && set DB_PASSWORD=frauenhaus && mvnw verify
```

`DB_PASSWORD` ist nötig, weil die Container-DB mit `--auth-host=scram-sha-256` läuft; der
Compose-Default des Schema-Eigentümers `frauenhaus_app` ist `frauenhaus`. `ops/verify-setup.ps1`
setzt genau diese Variablen selbst, deshalb braucht es dort keine Angabe.

**Unit-Tests** (Mockito/AssertJ, ohne Datenbank):

| Testklasse | Schwerpunkt |
|---|---|
| `AdminBootstrapTest` | initialer Admin, Zufallspasswort, Überspringen bei befüllter Tabelle |
| `AppUserServiceTest` | Passwortregeln, Namenskonflikte, Schutz des letzten aktiven Admins |
| `DbUserDetailsServiceTest` | Abbildung Rolle → `ROLE_*`, deaktivierte Benutzer |
| `RowLevelSecurityDataSourceTest` | Benutzerkontext je Connection, Aufräumen bei Fehlern |
| `DokumentServiceTest` | Upload-Validierung, Pfad-Traversal im Dateinamen, Größenlimit |
| `SpendeServiceTest` | Auflösung der Fremdschlüssel, Fehlerpfade, Suchnormalisierung |
| `StichwortServiceTest` | Zusammenstellen vs. Zusammenfassen (Reihenfolge Umhängen → Löschen) |
| `VerteilerServiceTest` | BCC-Versand, fehlende Empfänger/Absender, SMTP-Fehler |
| `BussgeldReportServiceTest` | Träger aus den Stammdaten, Summenzeilen |
| `DocumentCreationServiceTest` | 404 bei unbekannten Datensätzen |
| `ExcelUtilTest`, `WordTemplateServiceTest`, `BetragInWortenTest`, `DocumentCreationHelpersTest` | Dokument-Bausteine |

**Integrationstests** (`@SpringBootTest`, brauchen die Datenbank):

| Testklasse | Schwerpunkt |
|---|---|
| `StammdatenSucheIntegrationTest` | Volltextsuche über die Listen-Endpunkte |
| `VerteilerVersandIntegrationTest` | Verteiler-Versand über die REST-Schicht (SMTP gemockt) |
| `ApiSicherheitIntegrationTest` | 401 ohne Anmeldung, 403 für `/api/admin/**` ohne ADMIN, offener Health-Endpunkt |
| `RowLevelSecurityIntegrationTest` | verbindet sich **als `frauenhaus_backend`** und weist RLS + append-only-Historie nach |

> `RowLevelSecurityIntegrationTest` ist der einzige Test, der nicht als Schema-Eigentümer
> arbeitet – nur so ist RLS überhaupt beobachtbar, weil der Eigentümer davon ausgenommen ist.
> Fehlt die Rolle `frauenhaus_backend`, überspringt sich der Test selbst, statt fehlzuschlagen.

Die Vaadin-Views sind bewusst von der Coverage-Messung ausgenommen (`jacoco`-`excludes` bzw.
`sonar.coverage.exclusions`): reine UI-Verdrahtung ohne eigene Fachlogik, manuell abgenommen.

---

## Qualitätssicherung

### Setup verifizieren

Prüft nach Struktur-, Build- oder Datenbank-Änderungen alles auf einmal (Exitcode 0 = grün):

```bash
./ops/verify-setup.sh                                            # Linux / macOS
powershell -ExecutionPolicy Bypass -File ops\verify-setup.ps1     # Windows
```

| Schritt | Prüfung |
|---|---|
| 0. Datenbank | `docker compose up -d db`, danach warten bis Postgres Verbindungen annimmt (max. 5 min); ohne DB bricht das Skript sofort ab, statt in 33 Kontextfehlern zu enden |
| 1. Build | `mvnw clean verify -Pproduction` – Kompilierung, alle Tests, Produktions-Frontend |
| 2. Artefakt | JAR enthält `db/migration`, `db/testdata`, `application.yml`, `META-INF/VAADIN` – und **nicht** `db/init` oder die Frontend-Quellen |
| 3. Stack | `docker compose up -d --build`, Warten auf `/actuator/health` = `UP` |
| 4. Datenbank | Flyway-Historie enthält `V7`, Rolle `frauenhaus_backend` existiert, 11 RLS-Policies, kein `UPDATE` auf `*_aud`, kein `SELECT` auf die Flyway-Historie |

Optionen – Bash: `--recreate`, `--skip-build`, `--skip-docker`; PowerShell: `-Recreate`,
`-SkipBuild`, `-SkipDocker`. **recreate** setzt den Stack vorher mit `docker compose down -v` neu
auf: löscht das DB-Volume, prüft dafür aber den kompletten initdb-Pfad (Schema → Altdaten →
Übernahme → Rolle) samt Flyway-Baseline und zählt die übernommenen Mitglieder.

Voraussetzungen der Bash-Variante: `docker`, `curl` und entweder `jar` (JDK) oder `unzip`; beim
ersten Mal `chmod +x ops/verify-setup.sh` (und `git update-index --chmod=+x ops/verify-setup.sh`).

Manuell entspricht das:

```bash
docker compose up -d db
DB_PORT=15432 DB_PASSWORD=frauenhaus ./mvnw clean verify -Pproduction
docker compose down -v
docker compose up -d --build
curl http://localhost:8080/actuator/health
```

### Statische Analyse (SonarQube)

SonarQube läuft als **eigener** Compose-Stack (`docker-compose.sonarqube.yml`), damit der
Anwendungs-Stack unberührt bleibt. Komplette Analyse inklusive Testabdeckung:

```
powershell -ExecutionPolicy Bypass -File ops\sonar-analyse.ps1
```

Das Skript startet den Container, wartet auf `status=UP`, erzeugt über die SonarQube-API einen
Analyse-Token, fährt die Test-DB hoch und ruft `mvnw verify sonar:sonar` auf. Ergebnis:
http://localhost:9000/dashboard?id=frauenhaus-backend.

Optionen: `-SkipTests` (schneller, aber 0 % Coverage), `-AdminPassword <pw>` bzw.
`SONAR_ADMIN_PASSWORD` (nötig, sobald das Werks-Passwort `admin` geändert wurde), `-HostUrl <url>`.

Manuell:

```bash
docker compose -f docker-compose.sonarqube.yml -p sonarqube up -d
mvnw verify sonar:sonar -Dsonar.token=<token>
docker compose -f docker-compose.sonarqube.yml -p sonarqube down
```

Die Coverage liefert **JaCoCo** (`target/site/jacoco/jacoco.xml`, gebunden an
`sonar.coverage.jacoco.xmlReportPaths`) – sie entsteht nur, wenn die Tests wirklich laufen, dafür
muss die DB erreichbar sein. Tokens werden bewusst nur zur Laufzeit erzeugt und nie eingecheckt;
anonyme Analysen unterstützen aktuelle SonarQube-Versionen nicht mehr.

---

## Repository-Hygiene

- **Alle Nicht-Java-Quellen liegen unter `src/main/resources/`** – DB-Skripte (`db/migration`,
  `db/testdata`, `db/init`) und Vaadin-Frontend (`frontend/`). Die früheren Verzeichnisse
  `backend/db/` und `src/main/frontend/` sind entfallen; falls lokal noch vorhanden:
  ```cmd
  git rm -r --cached db src/main/frontend
  rmdir /s /q db src\main\frontend
  ```
  Für `db/init/05_sicherheit.sh` das Ausführungsrecht setzen
  (`git update-index --chmod=+x src/main/resources/db/init/05_sicherheit.sh`) – ohne das Bit führt
  der Postgres-Entrypoint das Skript per `source` aus, was zwar funktioniert, Fehler aber weniger
  deutlich meldet.
- **`ops/` bleibt außerhalb von `src/`** – reine Betriebs-/Werkzeug-Dateien, die zur Laufzeit nie
  gelesen werden und nicht in die JAR gehören. Begründung: [`ops/README.md`](ops/README.md).
- `.gitignore` schließt Build- und Werkzeug-Artefakte aus: `target/`, IDE-Dateien (`.idea/`,
  `*.iml`, `.settings/`), `.DS_Store` sowie das von Vaadin **selbst erzeugte** Frontend-Tooling
  (`package.json`, `package-lock.json`, `node_modules/`, `vite.generated.ts`,
  `src/main/resources/frontend/generated/`). Eine früher versehentlich eingecheckte, leere
  `package-lock.json` gehört entfernt: `git rm --cached package-lock.json`.
- Die Word-Vorlagen (`vorlagen/*.dot`) sind bewusst nicht im Repository und ebenfalls ignoriert.
- Der Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`) bleibt
  eingecheckt und legt die Maven-Version (3.9.9) für alle fest – bewusst **ohne**
  `maven-wrapper.jar` (`distributionType=only-script`). Das `Dockerfile` nutzt dieselbe Version;
  wird der Wrapper aktualisiert, muss das Basis-Image mitgezogen werden. Unter Linux/macOS braucht
  `mvnw` das Ausführungsrecht (`git update-index --chmod=+x mvnw`).

---

## Hinweise zum Entwickeln

- Im Entwicklungsmodus nutzt Vaadin sein vorkompiliertes Dev-Bundle – **npm/Node ist lokal nicht
  nötig**, solange nur Standard-Komponenten verwendet werden. Erst der Produktions-Build
  (`./mvnw package -Pproduction`, wie im `Dockerfile`) baut das optimierte Frontend-Bundle und
  lädt sich Node.js bei Bedarf selbst herunter.
- `frontend/generated/` erzeugt Vaadin bei jedem Build neu und ist ignoriert; gepflegt wird dort
  praktisch nur `index.html`.
- Der Dev-Stack enthält **Mailpit** als Fang-SMTP: alle „versendeten" Mails landen in der
  Demo-Inbox unter http://localhost:8025, nichts verlässt den Rechner. Ein lokal laufendes Backend
  erreicht ihn über `MAIL_HOST=localhost MAIL_PORT=1025`.

---

## Offene Punkte

- **Serienbriefe:** `FHSerienBrief.dot` / `SpendenQuittung*.dot` sind Word-Seriendruckdokumente
  (Datenquelle: xlsx von `/api/reports/serienbrief-adressen`).
- **SMTP in Produktion:** echten Mailserver über `MAIL_*` setzen; für Anbieter mit
  STARTTLS-/Auth-Pflicht (z.B. Gmail) fehlen in `application.yml` noch die passenden
  `spring.mail.properties`.
- **Backup: noch nicht in Betrieb.** `ops/pgbackrest.conf` ist ein dokumentierter Entwurf und im
  Compose-Stack bewusst *nicht* eingebunden (`postgres:18` enthält kein pgBackRest). Es fehlen ein
  Sidecar mit installiertem pgBackRest, die Archivierungs-Einstellungen (`wal_level`,
  `archive_mode`, `archive_command`), ein einmaliges `stanza-create`, ein Backup-Zeitplan und
  dokumentierte Restore-Tests – die Schritte stehen im Kopf der Konfigurationsdatei.