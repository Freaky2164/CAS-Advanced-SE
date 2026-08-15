---
title: "Architekturdiagramme der Vereinsverwaltung FH_MA"
subtitle: "Implementierte Code- und Laufzeitsicht — Ergänzung zur Seminararbeit der Arbeitsgruppe 3 (Architektur)"
author: "Nils Firschau (8993076) · Paul Faller (5567855) · Robin Steiner (9251426) · Ole Schildt (3504736)"
date: "15. August 2026"
module: "Seminararbeit im Modul CSC1200 „Advanced Software Engineering“"
professor: "M.Sc. Informatik · Prof. Dr. Holger D. Hofmann"
lang: de
toc: true
toc-depth: 3
numbersections: false
---

# Architekturdiagramme

Diese Diagramme dokumentieren die **tatsächlich implementierte** Architektur der
Frauenhaus-Verwaltung. Sie basieren auf dem Quellcode und ergänzen die in
[02-finale-architektur.md](02-finale-architektur.md) beschriebene Zielarchitektur
um die konkrete Code- und Laufzeitsicht.

**Technologie-Stand (verifiziert):** Spring Boot **4.1.0** · Java **25** ·
Vaadin **25.2.3** (nur freie `vaadin-core`-Komponenten) · PostgreSQL **18** ·
Hibernate Envers · Apache POI · Flyway · PostgreSQL-JDBC 42.7.13.

---

## 1. Systemarchitektur (Deployment / C4-Container)

```mermaid
graph TD
    subgraph Browser["Browser (im Vereins-LAN)"]
        UI_CLIENT["Vaadin-Client<br/>(server-side rendered,<br/>WebSocket/HTTP)"]
    end

    subgraph Proxy["Reverse Proxy (Produktion, vorgelagert)"]
        TLS["TLS-Terminierung<br/>nginx / caddy<br/>(nicht Teil des Stacks)"]
    end

    subgraph DockerHost["Laufzeitumgebung (docker-compose.yml)"]
        subgraph BackendC["backend (eclipse-temurin:25-jre, non-root)"]
            SB["Spring Boot 4.1.0 · Java 25<br/>FrauenhausApplication<br/>Port 8080"]
            VAADIN["Vaadin-UI 25.2.3<br/>(Produktions-Bundle, -Pproduction)"]
            REST["REST-API /api/**<br/>+ /actuator (health/info/metrics)"]
            VORLAGEN["Word-Vorlagen<br/>/app/vorlagen/*.dot"]
        end

        subgraph DbC["db (postgres:18)"]
            PG["PostgreSQL 18<br/>DB: frauenhaus"]
            SCHEMAS["Schemas: frauenhaus, app"]
            ROLES["Rollen:<br/>frauenhaus_app (Owner, DDL/Flyway)<br/>frauenhaus_backend (RLS, NOBYPASSRLS)"]
            INIT["initdb: 01_schema..05_sicherheit.sh<br/>Flyway: V1, V6, V7 (Baseline 5)"]
        end
    end

    subgraph Extern["Externe Dienste"]
        SMTP["SMTP-Server<br/>(optional, Verteiler-Versand)"]
    end

    subgraph Dev["Nur lokale Entwicklung (docker-compose.override.yml)"]
        DEV_DB["127.0.0.1:15432 → db:5432"]
        MAILPIT["mailpit (Demo-Inbox :8025)"]
    end

    UI_CLIENT -->|"HTTPS"| TLS
    TLS -->|"HTTP :8080"| SB
    UI_CLIENT -.->|"HTTPS :8080 (Dev, ohne Proxy)"| SB
    SB --- VAADIN
    SB --- REST
    SB -->|"JDBC (User: frauenhaus_backend)<br/>HikariCP, sslmode=prefer"| PG
    SB -.->|"Jakarta Mail (SMTP)"| SMTP
    SB -->|"liest .dot-Vorlagen"| VORLAGEN
    PG --- SCHEMAS
    PG --- ROLES
    PG --- INIT
    DEV_DB -.-> PG
    SB -.->|"Dev-Profil"| MAILPIT
```

### Deployment-Details

| Aspekt | Implementierung | Quelle |
|--------|-----------------|--------|
| Auslieferung | **Ein** Spring-Boot-Prozess liefert Vaadin-UI **und** REST-API aus — kein separater Web-/nginx-Container | `docker-compose.yml`, `pom.xml` |
| Frontend-Build | `-Pproduction` erzeugt optimiertes Vaadin-Bundle (Plugin lädt Node.js selbst) | `Dockerfile` |
| Laufzeit-Image | `eclipse-temurin:25-jre`, non-root User `app`, `EXPOSE 8080` | `Dockerfile` |
| TLS | In Produktion durch **vorgelagerten Reverse-Proxy** terminiert (nicht im Stack) | `docker-compose.yml` |
| Backend-Port | `${WEB_PORT:-8080}:8080` (UI + API gemeinsam) | `docker-compose.yml` |
| DB-Zugang App | `frauenhaus_backend` (NOSUPERUSER, NOCREATEDB, NOINHERIT, **NOBYPASSRLS**) | `V7__…rls.sql` |
| DB-Zugang Flyway | `frauenhaus_app` (Schema-Owner, DDL/GRANT) | `application.yml`, `docker-compose.yml` |
| Schema-Herkunft | `ddl-auto: validate`; Schema aus Flyway (`V1`, `V6`, `V7`) bzw. initdb-Skripten | `application.yml` |
| Upload-Limit | 10 MB (`spring.servlet.multipart` = `DokumentService.MAX_DATEIGROESSE`) | `application.yml` |
| SMTP | optional; ohne `spring.mail.host` kein `JavaMailSender` (nur Verteiler betroffen) | `application.yml` |
| Actuator | `health, info, metrics`; Mail-Health deaktiviert (SMTP optional) | `application.yml` |
| Dev-DB-Port | `127.0.0.1:15432` (vermeidet Konflikt mit lokalem Postgres) | `docker-compose.override.yml` |

---

## 2. Sicherheitsarchitektur (zwei Filterketten + RLS)

Die Anwendung trennt **UI** und **API** in zwei Spring-Security-Filterketten und
setzt die Zugriffskontrolle zusätzlich **in der Datenbank** durch (Defense-in-Depth).

```mermaid
graph TD
    REQ["HTTP-Request"]

    subgraph SEC["Spring Security (SecurityConfig)"]
        API_CHAIN["apiFilterChain @Order(1)<br/>securityMatcher /api/**, /actuator/**<br/>HTTP Basic · STATELESS · CSRF off<br/>401 ohne WWW-Authenticate"]
        UI_CHAIN["uiFilterChain @Order(2)<br/>VaadinSecurityConfigurer<br/>Formular-Login → LoginView<br/>Session-basiert · CSRF via Vaadin"]
        METHOD["@EnableMethodSecurity<br/>(feingranular je Service/Controller)"]
    end

    subgraph AUTHN["Authentifizierung"]
        DUDS["DbUserDetailsService<br/>loadUserByUsername()"]
        BCRYPT["BCryptPasswordEncoder"]
        BOOT["AdminBootstrap<br/>(initialer Admin, ENV oder Zufalls-PW)"]
        APPUSER[("app.app_user<br/>username, password_hash, role, enabled")]
    end

    subgraph DB["PostgreSQL — Defense in Depth"]
        RLS_DS["RowLevelSecurityDataSource<br/>set_config('app.benutzer',*)<br/>set_config('app.benutzer_rolle',*)<br/>pro Connection-Ausleihe"]
        ROLE_APP["Rolle frauenhaus_app<br/>Owner · DDL · Flyway"]
        ROLE_BE["Rolle frauenhaus_backend<br/>Least Privilege · NOBYPASSRLS"]
        POLICY["RLS-Policy 'benutzerkontext_erforderlich'<br/>USING/CHECK:<br/>current_setting('app.benutzer_rolle')<br/>IN ('ADMIN','SACHBEARBEITUNG')"]
        APPEND["Append-only Audit<br/>REVOKE UPDATE, DELETE auf *_aud + app.revinfo"]
    end

    REQ -->|"Pfad /api/** oder /actuator/**"| API_CHAIN
    REQ -->|"sonst (UI)"| UI_CHAIN
    API_CHAIN --> DUDS
    UI_CHAIN --> DUDS
    API_CHAIN -.-> METHOD
    UI_CHAIN -.-> METHOD
    DUDS --> APPUSER
    DUDS --> BCRYPT
    BOOT --> APPUSER
    API_CHAIN -->|"authentifiziert → JDBC"| RLS_DS
    UI_CHAIN -->|"authentifiziert → JDBC"| RLS_DS
    RLS_DS --> ROLE_BE
    ROLE_BE --> POLICY
    ROLE_APP -.->|"besitzt Schema, setzt Policies"| POLICY
    POLICY --- APPEND
```

### Sicherheitsmodell

| Ebene | Mechanismus | Quelle |
|-------|-------------|--------|
| API-Kette | `/api/**`, `/actuator/**`: HTTP Basic, `STATELESS`, CSRF aus; `/actuator/health/**` frei, `/api/admin/**` nur `ROLE_ADMIN` | `SecurityConfig.java` |
| UI-Kette | Vaadin `VaadinSecurityConfigurer`, Formular-Login über `LoginView`, Session; `/error` freigegeben | `SecurityConfig.java`, `LoginView.java` |
| 401-Verhalten | API sendet 401 **ohne** `WWW-Authenticate` → kein nativer Browser-Dialog | `SecurityConfig.java` |
| Passwörter | `BCryptPasswordEncoder` (adaptiv, gesalzen) | `SecurityConfig.java` |
| Benutzerquelle | `DbUserDetailsService` → `app.app_user` (Schema `app`) | `DbUserDetailsService.java` |
| Initial-Admin | `AdminBootstrap`: `APP_ADMIN_PASSWORD` oder einmalig geloggtes Zufalls-PW (kein hartes Default) | `AdminBootstrap.java` |
| Methodensicherheit | `@EnableMethodSecurity` (feingranular ergänzbar) | `SecurityConfig.java` |
| DB-Rollen | `frauenhaus_app` (Owner/DDL) vs. `frauenhaus_backend` (Least Privilege, `NOBYPASSRLS`) | `V7__…rls.sql` |
| RLS-Kontext | `RowLevelSecurityDataSource` setzt `app.benutzer` / `app.benutzer_rolle` je Connection (PreparedStatement, Fremdeingabe parametrisiert) | `RowLevelSecurityDataSource.java` |
| RLS-Policy | Zeilenfreigabe nur bei Rolle `IN ('ADMIN','SACHBEARBEITUNG')`; `current_setting(...,true)` → `NULL` statt Fehler | `V7__…rls.sql` |
| Append-only Audit | `REVOKE UPDATE, DELETE` auf `*_aud` + `app.revinfo` → Historie nicht manipulierbar | `V6__…append_only.sql`, `V7__…rls.sql` |

> **Ehrliche Grenze (aus dem Code kommentiert):** Die App-Rolle kann die
> RLS-Session-Variablen selbst per `set_config` setzen — RLS schützt vor
> *kontextlosem* Zugriff (versehentliches `psql`, naive Clients), ist aber keine
> harte Barriere gegen geleakte Backend-Zugangsdaten. Genau deshalb ist die
> Audit-Historie zusätzlich **append-only**. `app.app_user` bleibt bewusst
> **ohne** RLS (wird beim Login gelesen, bevor ein Kontext existiert).

---

## 3. Anwendungsschichten (Bausteinsicht)

```mermaid
graph TD
    BROWSER["<b>Browser</b> — Vaadin-Client<br/>(server-side gerendert)"]

    subgraph Backend["Spring Boot 4.1.0 Backend (ein Prozess)"]
        UI["<b>Präsentation</b> — Vaadin Views (@Route)<br/>LoginView · MainLayout · MitgliederView · SpendenView<br/>BussgelderView · VerwaltungView · StichworteView<br/>ReportsView · BenutzerView (ADMIN) · Dialoge"]
        WEB["<b>Web</b> — REST-Controller (/api)<br/>MeController · AppUserController · CRUD-Controller<br/>DokumentController · ReportController · Lookup-Controller<br/>ApiExceptionHandler (@RestControllerAdvice)"]
        SVC["<b>Service</b> — Geschäftslogik (@Transactional)<br/>Mitglied/Spende/Bussgeld · Dokument · Audit · AppUser<br/>Report · Stichwortsuche · DocumentCreation/WordTemplate (POI)<br/>Verteiler (SMTP) · Lookup-Services"]
        REPO["<b>Persistenz</b> — Spring Data JPA<br/>Mitglied/Spende/Bussgeld/Dokument/AppUser/Lookup-Repositories"]
        DOM["<b>Domäne</b> — JPA-Entities<br/>Schema frauenhaus: Mitglied · Spende · Bussgeld · Eingang<br/>Dokument · Verein · Gericht · Stichwort · Anrede · …<br/>Schema app: AppUser · Revision"]

        UI --> WEB
        WEB --> SVC
        UI -.->|"direkt"| SVC
        SVC --> REPO
        REPO --> DOM
    end

    CROSS["<b>Querschnitt</b><br/>SecurityConfig (2 Filterketten) · RLS-DataSource<br/>Hibernate Envers (Audit) · Flyway (V1, V6, V7)"]

    DB[("PostgreSQL 18<br/>frauenhaus.* · app.* · *_aud")]
    SMTP["SMTP-Server"]

    BROWSER -->|"Vaadin (Session)"| UI
    BROWSER -->|"REST (HTTP Basic)"| WEB
    SVC -->|"Jakarta Mail"| SMTP
    REPO -->|"JDBC über RLS-Wrapper"| DB
    DOM --> DB
    CROSS -.->|"absichern · auditieren · migrieren"| Backend
```

> **Zusammenspiel UI ↔ API.** Die Vaadin-Views sind reine serverseitige
> UI-Verdrahtung; fachliche Operationen laufen über die REST-Controller bzw.
> direkt über die Service-Schicht im selben Prozess. Beide Eintrittspunkte teilen
> sich Services, Repositories, Domänenmodell und die RLS-abgesicherte DataSource.

---

## 4. Domänenmodell (Entity-Relationship)

Abgeleitet aus den JPA-Entities (`de.frauenhaus.domain`, Schema `frauenhaus`).
`@Audited`-Entities werden von Envers in `*_aud`-Tabellen historisiert.

```mermaid
erDiagram
    MITGLIED ||--o{ SPENDE : "leistet (Spender)"
    VEREIN   ||--o{ SPENDE : "erhaelt"
    SPENDENART ||--o{ SPENDE : "typisiert"
    SPENDENTYP ||--o{ SPENDENART : "gruppiert"
    GERICHT  ||--o{ BUSSGELD : "verhaengt"
    VEREIN   ||--o{ BUSSGELD : "erhaelt"
    BUSSGELD ||--o{ EINGANG : "hat Zahlungseingaenge"
    MITGLIED }o--o{ STICHWORT : "stichwort_person"
    MITGLIED }o--o{ VEREIN : "verein_mitglied"

    MITGLIED {
        Long id PK
        String anrede "Lookup ANREDE (Name)"
        String vorname
        String name
        String strasse
        String plz
        String ort
        String email
        boolean foerderverein
        boolean frauenhaus
    }
    SPENDE {
        Long id PK
        Long mitglied FK
        String spendenart FK
        String verein FK
        date datum
        decimal betrag
    }
    BUSSGELD {
        Long id PK
        Long gericht FK
        String verein FK
        String status "Lookup BUSSGELDSTATUS"
        String aktenzeichen
        date datum
        date zieldatum
        decimal betrag
        boolean bezahlt
    }
    EINGANG {
        Long id PK
        Long bussgeld FK
        date datum
        decimal betrag
    }
    VEREIN {
        String name PK
        String bezeichnung
    }
    GERICHT {
        Long id PK
        String bezeichnung
        String ort
    }
    STICHWORT {
        String name PK
    }
    SPENDENART {
        String name PK
        String spendentyp FK
    }
    SPENDENTYP {
        String name PK
    }
    ANREDE {
        String name PK
    }
    BUSSGELDSTATUS {
        String name PK
    }
    DOKUMENT {
        Long id PK
        EntityTyp entity_typ "MITGLIED|VEREIN|BUSSGELD|SPENDE|GERICHT"
        String entity_id
        String dateiname
        String content_type
        bytea inhalt
        timestamptz hochgeladen_am
        String hochgeladen_von
    }
```

> **`Dokument` ist polymorph** (kein FK): Die Zuordnung erfolgt über
> `entity_typ` (Enum) + `entity_id` mit Index `idx_dokument_entity`. Der Inhalt
> liegt als `bytea` in der Datenbank (ein gemeinsamer Backup-Pfad, ADR-008).

### Entities, Audit & Beziehungen

| Entity | Schlüssel | `@Audited` | Audit-Tabelle | Beziehungen |
|--------|-----------|:----------:|---------------|-------------|
| `Mitglied` | `Long id` | ✅ | `mitglied_aud` | M:N `Stichwort` (`stichwort_person`), M:N `Verein` (`verein_mitglied`) |
| `Spende` | `Long id` | ✅ | `spende_aud` | M:1 `Mitglied` (Ziel *nicht* auditiert), M:1 `Spendenart`, M:1 `Verein` |
| `Bussgeld` | `Long id` | ✅ | `bussgeld_aud` | M:1 `Gericht`, M:1 `Verein`, 1:N `Eingang` (cascade, orphanRemoval) |
| `Eingang` | `Long id` | ❌ | — | M:1 `Bussgeld` |
| `Verein` | `String name` | ✅ | `verein_aud` | — |
| `Gericht` | `Long id` | ✅ | `gericht_aud` | — |
| `Dokument` | `Long id` | ❌ | — | polymorph (`entity_typ` + `entity_id`) |
| `Stichwort` / `Anrede` / `Spendenart` / `Spendentyp` / `Bussgeldstatus` | `String name` | ❌ | — | Lookups |
| `AppUser` | `Long id` | ❌ | — | Schema `app` |

### RLS-geschützte Tabellen (Policy `benutzerkontext_erforderlich`)

`frauenhaus.mitglied`, `spende`, `bussgeld`, `eingang`, `stichwort_person`,
`verein_mitglied`, `dokument`, `mitglied_aud`, `spende_aud`, `bussgeld_aud`,
`app.revinfo`.

**Bewusst ohne RLS:** `app.app_user` (Login-Henne-Ei) sowie reine Lookups ohne
Personenbezug (`anrede`, `verein`, `gericht`, `spendenart`, `spendentyp`,
`stichwort`, `bussgeldstatus`, `verein_aud`, `gericht_aud`).

---

## 5. Authentifizierung & Autorisierung (Sequenz)

Zwei Eintrittspunkte mit unterschiedlicher Session-Politik, aber gemeinsamem
Benutzerspeicher und gemeinsamem RLS-Kontext.

```mermaid
sequenceDiagram
    actor User
    participant UI as Vaadin UI (LoginView)
    participant API as REST-Client (/api)
    participant SEC as SecurityFilterChain
    participant DUDS as DbUserDetailsService
    participant DB as app.app_user
    participant SCH as SecurityContextHolder
    participant RLSDS as RowLevelSecurityDataSource
    participant PG as PostgreSQL (RLS)

    rect rgb(230, 245, 255)
        Note over User,PG: UI-Login (Session, Formular)
        User->>UI: Benutzername + Passwort
        UI->>SEC: POST /login (uiFilterChain @Order 2)
        SEC->>DUDS: loadUserByUsername(name)
        DUDS->>DB: SELECT username, password_hash, role, enabled
        DB-->>DUDS: AppUser
        DUDS-->>SEC: UserDetails
        SEC->>SEC: BCrypt.matches()
        alt Erfolg
            SEC->>SCH: setAuthentication() + HTTP-Session
            SEC-->>UI: Redirect auf Zielview (MainLayout)
        else Fehler
            SEC-->>UI: Redirect /login?error → LoginView zeigt Fehler
        end
    end

    rect rgb(255, 245, 230)
        Note over User,PG: API-Request (STATELESS, HTTP Basic) + RLS
        API->>SEC: GET /api/mitglieder (apiFilterChain @Order 1)<br/>Authorization: Basic ...
        SEC->>DUDS: loadUserByUsername(name)
        DUDS->>DB: SELECT ...
        DB-->>DUDS: AppUser
        SEC->>SEC: BCrypt.matches()
        SEC->>SCH: setAuthentication (nur für diesen Request)
        Note over RLSDS: Connection-Ausleihe aus HikariCP
        RLSDS->>SCH: getAuthentication() → Name + Rolle
        RLSDS->>PG: set_config('app.benutzer', name),<br/>set_config('app.benutzer_rolle', rolle)
        Note over PG: Policy prüft Rolle IN ('ADMIN','SACHBEARBEITUNG')
        PG-->>RLSDS: Verbindung mit Kontext (sonst 0 Zeilen)
        SEC-->>API: 200 (Daten) / 401 ohne WWW-Authenticate
    end

    rect rgb(230, 255, 230)
        Note over User,PG: Autorisierung
        Note over SEC: /api/admin/** → hasRole('ADMIN')
        Note over UI: BenutzerView nur bei ROLE_ADMIN im Drawer
        Note over SEC: @EnableMethodSecurity ergänzt Service-Ebene
    end

    rect rgb(255, 230, 230)
        Note over User,PG: Logout
        User->>UI: Abmelden (AuthenticationContext.logout())
        UI->>SEC: Session invalidieren
        SEC-->>UI: Redirect /login
    end
```

### Auth-Flow Details

| Aspekt | Implementierung | Quelle |
|--------|-----------------|--------|
| UI-Login | Vaadin `LoginForm` postet auf `login`; Session-basiert | `LoginView.java`, `SecurityConfig.java` |
| API-Login | HTTP Basic je Request, `SessionCreationPolicy.STATELESS` | `SecurityConfig.java` |
| `/api/me` | Liefert `username` + `roles` des angemeldeten Principals | `MeController.java` |
| Rollen | `ROLE_ADMIN`, `ROLE_SACHBEARBEITUNG` (RBAC) | `SecurityConfig.java`, `V7__…rls.sql` |
| Admin-Sichtbarkeit | `BenutzerView` nur im Drawer, wenn `ROLE_ADMIN` | `MainLayout.java` |
| RLS-Kontext | pro Connection gesetzt; leer ohne Anmeldung (kein Verschleppen im Pool) | `RowLevelSecurityDataSource.java` |
| Audit-Bearbeiter | `AuditRevisionListener` schreibt Benutzernamen aus dem `SecurityContext` (sonst `system`) | `AuditRevisionListener.java` |

---

## 6. Dokument- & Report-Erzeugung (Sequenz)

Serverseitige Erzeugung von Word-/Excel-Dokumenten (Apache POI, headless — kein
Office/Outlook) sowie Verteiler-Versand per SMTP.

```mermaid
sequenceDiagram
    actor SB as Sachbearbeiter
    participant RC as ReportController (/api/reports)
    participant DCS as DocumentCreationService
    participant WTS as WordTemplateService (POI XWPF)
    participant REPO as Repositories
    participant VS as VerteilerService
    participant SUS as StichwortsucheService
    participant SMTP as SMTP-Server
    participant PG as PostgreSQL (RLS)

    rect rgb(230, 245, 255)
        Note over SB,PG: Spendenbescheinigung / Bußgeldbestätigung (.docx)
        SB->>RC: GET /spendenquittung/{spendeId}
        RC->>DCS: spendenBescheinigung(spendeId)
        DCS->>REPO: findById(...)
        REPO->>PG: SELECT (RLS-Kontext gesetzt)
        PG-->>REPO: Spende + Spender + Verein
        DCS->>WTS: neuesDokument(), absatz(), ortUndDatum(), toBytes()
        WTS-->>DCS: byte[] (.docx)
        DCS-->>RC: byte[]
        RC-->>SB: 200 application/vnd...wordprocessingml (Download)
    end

    rect rgb(255, 245, 230)
        Note over SB,PG: Übersichten & Stichwortsuche (.xlsx)
        SB->>RC: GET /bussgeld-uebersicht bzw. /stichwortsuche.xlsx
        RC->>SUS: suchenAlsExcel(stichworte, ...)
        SUS->>REPO: Query (RLS)
        REPO->>PG: SELECT
        SUS-->>RC: byte[] (.xlsx, ExcelUtil / POI)
        RC-->>SB: 200 (Download)
    end

    rect rgb(230, 255, 230)
        Note over SB,PG: Verteiler-Versand (Serien-E-Mail)
        SB->>RC: POST /verteiler/versenden {stichworte, betreff, text}
        RC->>VS: versenden(stichworte, betreff, text)
        VS->>REPO: emails(stichworte)
        REPO->>PG: SELECT Empfänger (RLS)
        VS->>SMTP: Jakarta Mail (Absender aus app.mail.absender)
        SMTP-->>VS: akzeptiert
        VS-->>RC: VersandErgebnis(empfaengerAnzahl)
        RC-->>SB: 200 { empfaengerAnzahl }
    end
```

### Dokument-Funktionen

| Endpunkt / Funktion | Ergebnis | Umsetzung | Quelle |
|---------------------|----------|-----------|--------|
| `/spendenquittung/{id}`, `/bussgeld-bestaetigung/{id}` | `.docx` | `DocumentCreationService` → `WordTemplateService` (POI XWPF) | `ReportController.java`, `DocumentCreationService.java` |
| `/bussgeld-uebersicht`, `/bussgeld-detail`, `/spenden-uebersicht` | `.xlsx` | `BussgeldReportService` / `ExcelUtil` (POI) | `ReportController.java` |
| `/stichwortsuche`, `/stichwortsuche.xlsx` | JSON / `.xlsx` | `StichwortsucheService` | `StichwortsucheService.java` |
| `/serienbrief`, `/serienbrief-adressen` | `.docx` / Adressliste | `VerteilerService.serienbrief/adressen` | `VerteilerService.java` |
| `/verteiler-emails`, `/verteiler/versenden` | E-Mail-Liste / Versand | `VerteilerService` (SMTP) | `VerteilerService.java` |
| Dokument-Upload/Download | `bytea` | `DokumentController` → `DokumentService` (polymorph, max. 10 MB) | `DokumentController.java` |

---

## Zusammenfassung der Abweichungen von der Planungsdoku

| Thema | Planung (Seminararbeit / ADRs) | Implementierung (dieser Stand) |
|-------|--------------------------------|--------------------------------|
| UI-Framework | Vaadin (ADR-003) | **Vaadin 25.2.3** — wie geplant |
| REST-Schicht | „nicht Teil der initialen Architektur" (ADR-001) | **vorhanden** (`/api/**`, eigene STATELESS-Filterkette) |
| Authentifizierung | Session-basiert (Vaadin) | **UI: Session · API: HTTP Basic** (zwei Filterketten) |
| DB-Absicherung | BCrypt + RBAC, Prepared Statements | **zusätzlich RLS + append-only Audit + Least-Privilege-Rolle** |
| Spring Boot / Java | Spring Boot 4.x / Java 25 | **4.1.0 / 25** |

Diese Ergänzungen (REST-API, RLS, append-only Historie) sind konsequente
Härtungen und wurden im Zuge des Gruppen-Reviews eingeführt; sie stärken das in
ADR-004/008/009 formulierte Sicherheits- und Persistenzkonzept, ohne den
Architekturstil (3-Schichten, On-Premises, Vaadin) zu verändern.
