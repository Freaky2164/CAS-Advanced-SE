# Migration Compucrash → Spring Boot + PostgreSQL

Ziel-Stack: Spring Boot (Backend, REST-API), PostgreSQL, Frontend beliebig (vorerst offen).
Fokus: **Datensicherheit, Backup & Recovery, einfache Wartung.**

## 1. Was gelöscht wurde

- **Gesamte Swing-UI** (~60 Klassen: `C*Frame*`, `CDisplayField*`, `CTable*`, `CButton*`, Dialoge, `CMediator`/`CColleague`/`C*Parent`, `CStart`, `CSplashScreen`, `CLoginFrame`, `CPropertyManager`, `Test.java`) – wird durch neues Frontend + REST-API ersetzt.
- **Tote DB-Connectors**: Oracle, MySQL, SQL Server (`CManagingOracle/MySQL/SQLServer`, `C*DataManagingOracle/MySQL`) – Ziel ist nur noch PostgreSQL.
- **`ext/` (alte JARs)** und `run.bat` – Dependencies kommen künftig über Maven/Gradle.

⚠️ Die alte App ist damit **nicht mehr lauffähig**. Der Rest dient nur als Referenz für den Neubau.

## 2. Was bleibt und warum (Referenz für den Neubau)

| Behalten | Enthält | Wird zu |
|---|---|---|
| `frauenhaus/*` | Die eigentliche Fachlogik: Bußgelder, Spenden(-quittungen), Serienbriefe, Verteiler, Stichwortsuche + `vorlagen/` (Word/Excel-Templates) | Spring `@Service`-Klassen (1 Service pro Use Case) |
| `compucrash/CDataObjectFactory`, `CDataObject`, `CInfoDataObject`, `CListDataObject` | Das Datenmodell (Tabellen/Attribute werden zur Laufzeit aus DB-Metadaten gelesen) | Explizite JPA-Entities + Flyway-Migrationsskripte |
| `compucrash/CDataManager`, `CManagingDatabase`, `CManagingPostgres`, `C*DataManagingDatabase`, `C*DataManagingSQLServer`* | SQL-Queries und CRUD-Logik | Spring Data JPA Repositories |
| `compucrash/CCommand`, `CActionCommand`, `CReport` | Basistypen der Fachlogik | entfallen (durch Services ersetzt) |
| `compucrash/ImportExcel`, `CProperties`, `CMessage`, `CNull`, `Utilities` | Excel-Import, Hilfsklassen | Import-Service (modernes Apache POI) |
| `de/must/util/WordProcessing.java` | Word-Ansteuerung für Serienbriefe | docx4j / POI-XWPF (s.u.) |
| `Compucrash.ini`, `pc-6.ini` | Konfiguration, Objektnamen des Datenmodells | `application.yml` + Umgebungsvariablen |

*Die `...SQLServer`-DataManaging-Klassen blieben, weil `CManagingPostgres` sie wiederverwendet – sie enthalten die tatsächlich genutzten SQL-Statements.

## 3. Was umgeschrieben werden muss

### Datenzugriff
Der gesamte handgeschriebene JDBC-Layer (String-konkatenierte SQL-Statements, Singleton-`CDataManager`, metadatengetriebene `CDataObjectFactory`) wird ersetzt durch Spring Data JPA mit explizitem, versioniertem Schema (Flyway). Das eliminiert die größte Sicherheitslücke des Altsystems: **SQL-Injection durch String-Konkatenation** (z.B. `"SELECT " + validateSqlExpression(init)` in `CManagingPostgres`). JPA/Prepared Statements parametrisieren alles.

### Authentifizierung & Autorisierung
Alt: `CLoginFrame` mit DB-Zugangsdaten, **Passwörter im Klartext in `Compucrash.ini`**. Neu: Spring Security mit BCrypt-gehashten Benutzerpasswörtern, Rollen (z.B. `ADMIN`, `SACHBEARBEITUNG`), Session- oder JWT-basierte Absicherung der REST-API. DB-Credentials nur noch als Umgebungsvariablen/Secrets, nie im Repo.

### Fachlogik (`frauenhaus/`)
Die Commands/Reports mischen Fachlogik mit `java.awt`/`javax.swing` (Dialoge, Clipboard) und Windows-COM (Outlook via `ch.kova.connector`, Word/Excel-Pfade aus der ini). Umschreiben zu:

- Reports (Bußgeld-Übersicht/-Detail, Spendenübersicht): serverseitige Generierung mit aktuellem Apache POI (xlsx statt xls) oder JasperReports; Auslieferung als Download über REST.
- Serienbriefe/Spendenquittungen: Word-COM ersetzen durch docx4j oder POI-XWPF mit den bestehenden Vorlagen aus `frauenhaus/vorlagen/`.
- Outlook-Anbindung ersetzen durch SMTP (Jakarta Mail) – COM läuft auf einem Server nicht.
- Excel-Import (`ImportExcel`): Upload-Endpoint + POI.

### Konfiguration
`*.ini`-Dateien → `application.yml` mit Spring Profiles (`dev`, `prod`); Fensterpositionen etc. entfallen (Frontend-Sache).

## 4. Fokusthemen

### Datensicherheit
Sensible Personendaten (Frauenhaus-Kontext → DSGVO Art. 9, besondere Schutzpflicht):

- TLS für API und DB-Verbindung (`sslmode=verify-full`), PostgreSQL `scram-sha-256`-Auth.
- Verschlüsselung at rest (Disk-/Tablespace-Ebene oder `pgcrypto` für besonders sensible Spalten wie Adressen betroffener Frauen).
- Least-Privilege-DB-User: App-User ohne DDL-Rechte; separater Migrations-User für Flyway.
- Audit-Logging über Hibernate Envers oder DB-Trigger (wer hat wann welchen Datensatz geändert – ersetzt `CDisplayFieldHistoryBean`).
- Secrets via Umgebungsvariablen/Vault; keine Credentials in Dateien.

### Backup & Recovery
- **pgBackRest** (oder `pg_basebackup` + WAL-Archivierung): tägliche Voll-/stündliche inkrementelle Backups, Point-in-Time-Recovery.
- Backups verschlüsselt und auf getrenntem Speicherort (3-2-1-Regel).
- Dokumente/Vorlagen (heute Filesystem) entweder als `bytea`/Large Object in die DB (ein Backup-Pfad für alles) oder separates Verzeichnis-Backup.
- Restore-Prozedur dokumentieren und **regelmäßig testen** (z.B. quartalsweise Probe-Restore in Staging).

### Einfache Wartung
- Ein DB-Dialekt statt vier → der komplette `CManaging*`-Abstraktionszoo entfällt.
- Flyway: Schema-Änderungen versioniert und reproduzierbar statt Laufzeit-Metadaten-Magie der `CDataObjectFactory`.
- Maven/Gradle statt `ext/`-JARs von 2004; Dependabot/Renovate für Updates.
- Spring Boot Actuator: Health-Checks, Metriken, Log-Level zur Laufzeit.
- Docker Compose (App + PostgreSQL) für identische Dev-/Prod-Umgebung.
- Schichtenarchitektur: Controller → Service → Repository; Tests mit Testcontainers (echte PostgreSQL in CI).

## 5. Stand 04.07.2026: Schritte 2–7 umgesetzt

Das neue Backend liegt in `backend/` (Spring Boot 3.3, Java 17, Maven). Umgesetzt: Flyway-Baseline (aus dem Alt-SQL rekonstruiert, da keine laufende DB verfügbar war – vor Produktivnahme gegen einen `pg_dump --schema-only` der echten DB abgleichen!), JPA-Entities + Repositories mit parametrisierten Queries (SQL-Injection behoben), Spring Security mit BCrypt und Admin-Bootstrap, portierte Fachlogik (Bußgeld-Übersicht/-Detail/-Bestätigung, Spendenübersicht/-quittungen inkl. Betrag in Worten, Verteiler, Serienbrief-Adressen, Stichwort-Zusammenstellen/-fassen) als Services mit REST-Endpoints, pgBackRest-Backup-Konzept (`backend/ops/`), Dockerfile + docker-compose. Der komplette Alt-Code wurde gelöscht; die Word-/Excel-Vorlagen liegen in `backend/vorlagen-alt/`. `ImportExcel.java` entfiel ersatzlos (schrieb in `biopharm.mp52` – Fremdcode aus einem anderen Projekt).

**Update:** Die Baseline wurde inzwischen gegen das MSSQL-Backup (`Backup_MSSQL_FH_anonymisiert.bak`) abgeglichen – ergänzt wurden die Tabellen `anrede`, `spendentyp`, `verein_mitglied` sowie die Spalten `mitglied.tel1/tel2/fax/foerderverein/frauenhaus/bemerkung` und `bussgeld.zieldatum/bezahlt`. Die Datenübernahme ist in `backend/ops/DATENUEBERNAHME.md` beschrieben; `fh_MA.accdb` ist nur ein Access-Frontend (ODBC-Links, siehe `fh.dsn`) ohne eigene Daten.

Noch offen: `mvn verify` lokal ausführen (in der Sandbox war kein Maven/JDK 17 verfügbar), Datenübernahme aus der Alt-DB, CRUD-Endpoints fürs Frontend, Git-Repo initialisieren.

**Update 12.07.2026 – Datenübernahme und Vorlagen umgesetzt:**

- **Datenübernahme:** Das Backup des Altsystems (`Code/data.sql`, rebasedata-Dump mit
  `public._frauenhaus_*`-Tabellen, alles untypisiert als varchar) wird beim ersten
  `docker compose up` automatisch eingespielt und von
  `backend/db/init/04_datenuebernahme.sql` in das Zielschema `frauenhaus.*` überführt
  (Typkonvertierung, NULL-Bereinigung, Ableitung `bussgeld.bezahlt` aus dem Status,
  Ersatz-Spendenart `unbekannt`, Sequenz-Anpassung). Ergebnis: 2325 Mitglieder,
  8161 Spenden, 219 Bußgelder, 695 Eingänge, 13014 Stichwort-Zuordnungen;
  7 Alt-Spenden ohne Datum/Träger werden übersprungen (siehe DB-Log).
- **Schema-Verwaltung:** Statt Flyway legen die `docker-entrypoint-initdb.d`-Skripte
  das Schema an (`V1__baseline_schema.sql` + Datenübernahme); Flyway wurde aus
  pom.xml/application.yml entfernt, Hibernate validiert weiterhin (`ddl-auto: validate`).
- **Dokumente aus Vorlagen:** `DocumentCreationService` befüllt die Original-Word-Vorlagen
  aus `backend/vorlagen/` über ihre Lesezeichen mit POI-HWPF (kein Word/COM nötig):
  Bußgeldbestätigungen aus `FHBG.dot`/`FVBG.dot`, Spendenbescheinigungen aus den
  `FHSB*`/`FVSB*`-Vorlagen je Träger und Spendentyp (Dauerspenden mit Jahressumme und
  Einzelbeträgen, Betrag in Worten). Die Endpunkte `/api/reports/bussgeld-bestaetigung/{id}`
  und `/api/reports/spendenquittung/{id}` liefern jetzt .doc-Dateien aus diesen Vorlagen.

**Update 12.07.2026 (abends) – PostgreSQL 18, Row Level Security, Aufzählungen:**

- **PostgreSQL 18** statt 16 (`docker-compose.yml`): Das offizielle 18er-Image legt die
  Daten unter `/var/lib/postgresql/18/docker` ab, Volume-Mount und `pgbackrest.conf`
  (`pg1-path`) wurden entsprechend angepasst.
- **Row Level Security (DSGVO):** Die Anwendung verbindet sich als eingeschränkte Rolle
  `frauenhaus_backend` (kein DDL, kein BYPASSRLS, `db/init/05_sicherheit.sh`). Alle
  Tabellen mit personenbezogenen Daten tragen RLS-Policies, die Zeilen nur bei gesetztem
  Benutzerkontext freigeben; `RowLevelSecurityDataSource` setzt diesen Kontext pro
  Connection-Ausleihe aus dem Spring-Security-Login. Direktzugriffe mit den
  Backend-Zugangsdaten ohne App (psql, kompromittierter Client) sehen null Zeilen.
  Damit ist auch das Fokusthema "Least-Privilege-DB-User" aus Abschnitt 4 umgesetzt.
- **Aufzählungen in Vorlagen:** Mehrzeilig befüllte Lesezeichen (Zahlungsliste,
  Einzelspenden) verloren ab der zweiten Zeile die Listenformatierung, weil neu
  eingefügte Absatzmarken im Word-Binärformat keine Absatz-Eigenschaften (PAPX) haben.
  `DocumentCreationHelpers` klont jetzt die Formatierung des Vorlagen-Absatzes auf alle
  eingefügten Zeilen – Bulletpoints/Nummerierungen werden je Zeile weitergeführt.
- **Ein Einstiegspunkt, TLS terminiert im nginx:** Der Compose-Stack enthält jetzt einen
  `web`-Container (nginx + Angular-Build, `frontend/Dockerfile`), der als einziger Ports
  veröffentlicht (80 → Redirect, 443 → App) und `/api` intern auf `backend:8080` proxyt.
  Backend und DB sind von außen nicht mehr erreichbar (Dev-Mappings nur über
  `docker-compose.override.yml`, an 127.0.0.1 gebunden; DB auf Host-Port 15432, weil
  5432 lokal oft belegt ist). TLS: echtes Zertifikat per Volume-Mount, sonst erzeugt
  `frontend/tls-selbstsigniert.sh` beim ersten Start ein selbstsigniertes – die
  Basic-Auth-Zugangsdaten laufen damit nie mehr im Klartext übers Netz.

## 6. Ursprünglich empfohlene Reihenfolge

1. Git-Repo initialisieren (aktuell keine Versionskontrolle!).
2. Schema aus laufender Postgres-DB extrahieren (`pg_dump --schema-only`) → Flyway-Baseline.
3. Spring-Boot-Projekt: Entities + Repositories für die Kernobjekte (Person, Spende, Bußgeld, Mitglied, Verein, Stichwort, Gericht – siehe Objektnamen in `pc-6.ini`).
4. Spring Security + Benutzertabelle mit gehashten Passwörtern.
5. Fachlogik aus `frauenhaus/` als Services portieren (erst Reports, dann Serienbriefe).
6. Backup-Konzept (pgBackRest) parallel zur ersten Prod-Umgebung aufsetzen.
7. Danach: Alt-Code-Reste (`compucrash/`, `frauenhaus/`, `de/`) löschen, sobald portiert.
