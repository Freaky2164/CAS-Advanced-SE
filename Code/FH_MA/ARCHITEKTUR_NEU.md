# Neue Architektur – Frauenhaus Adress- und Bußgeldverwaltung

## 1. IST-Zustand – Analyse des bestehenden Systems

### 1.1 Systemübersicht

Das bestehende System ist eine **Legacy Java-Swing-Desktop-Anwendung** aus ca. 2005–2013
(„Compucrash"-Framework), die eine **klassische 2-Schichten-Architektur** (Fat Client +
Datenbankserver) implementiert.

```
┌─────────────────────────────────┐     JDBC/TCP:1433      ┌──────────────────────┐
│  Java Swing Fat Client          │ ─────────────────────► │  MS SQL Server       │
│  (pro Arbeitsplatz installiert) │                        │  Express Edition     │
│  - CStart / CLoginFrame         │ ◄───────────────────── │  (pc-5)              │
│  - CMainFrame / CListFrame      │                        │  Schema: frauenhaus  │
│  - CInfoFrame / CReport*        │                        │  Schema: compucrash  │
└─────────────────────────────────┘                        └──────────────────────┘
        ▲
        │ .ini Dateien (frauenhaus.ini, pc-6.ini)
        │ Y:\ Netzlaufwerk (Reports, Vorlagen)
        ▼
┌─────────────────────────────────┐
│  Lokales Dateisystem + MS Excel │
│  Word (.dot-Vorlagen)           │
│  Adobe Acrobat (PDF)            │
└─────────────────────────────────┘
```

### 1.2 Technologie-Stack (IST)

| Komponente       | Technologie                       | Version (ca.)    |
|------------------|-----------------------------------|------------------|
| Laufzeit         | Java SE                           | 1.4 – 1.5        |
| UI               | Java Swing (AWT)                  | –                |
| DB-Treiber       | jTDS                              | 1.1 (2004)       |
| Datenbank        | MS SQL Server Express             | 2008/2012        |
| Reporting        | Apache POI (Excel)                | 2.5.1 (2004)     |
| Briefvorlagen    | MS Word .dot                      | Office 11/14     |
| Outlook-Integration | joutlookconnector              | v14              |
| Konfiguration    | .ini-Dateien (pro PC)             | –                |
| Build            | Classpath-Batch (run.bat)         | –                |

### 1.3 Identifizierte Schwachstellen des IST-Zustands

| Bereich             | Problem                                                                                  |
|---------------------|------------------------------------------------------------------------------------------|
| **Datensicherheit** | Passwörter im Klartext in .ini-Dateien gespeichert (`dbpwd=...`)                        |
| **Datensicherheit** | SQL-Injection-Anfälligkeit durch String-Konkatenation in allen SQL-Queries              |
| **Datensicherheit** | DB-Zugangsdaten direkt auf Client-PCs (frauenhaus.ini, pc-6.ini)                       |
| **Datensicherheit** | Keine Transportverschlüsselung (unverschlüsselte JDBC-Verbindung)                      |
| **Datensicherheit** | Kein Rollenkonzept auf Anwendungsebene (Benutzerrechte nur in DB-Tabellen)             |
| **Backup & Recovery** | SQL Server Express: max. 4 GB Datenbankgröße, keine SQL Agent-Jobs                  |
| **Backup & Recovery** | Keine dokumentierte Backup-Strategie erkennbar                                       |
| **Backup & Recovery** | Reports und Vorlagen auf Netzlaufwerk (Y:), kein automatisches Backup                |
| **Wartung**         | Fat-Client-Deployment: jede Installation auf jedem PC einzeln                          |
| **Wartung**         | Veraltete Bibliotheken (POI 2004, jTDS 2004)                                           |
| **Wartung**         | Hardcodierte Pfade in .bat-Dateien und .ini-Dateien                                    |
| **Wartung**         | Kein Logging-Framework, nur System.out.println()                                       |
| **Wartung**         | Per-PC-Konfigurationsdateien – schwierig konsistent zu halten                          |
| **Wartung**         | `System.exit(0)` bei Fehlern – kein graceful error handling                            |

---

## 2. SOLL-Zustand – Neue Architektur

### 2.1 Architekturprinzipien

Die neue Architektur folgt folgenden Leitprinzipien:

- **Windows-native**: Alle Komponenten laufen als Windows-Dienste oder nativ unter Windows
- **Zentralisierung**: Keine Installation auf Client-PCs (Ausnahme: Browser)
- **Defence in Depth**: Mehrschichtige Sicherheitsstrategie
- **Automatisiertes Backup**: SQL Server Agent + Windows Task Scheduler
- **Einfache Wartung**: Einheitliche Konfiguration, Logging, Updates zentral

### 2.2 Neue 3-Schichten-Architektur

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            WINDOWS SERVER / PC                              │
│                                                                             │
│  ┌─────────────────────────────────┐                                        │
│  │  PRÄSENTATIONSSCHICHT           │                                        │
│  │  Angular SPA (Browser)          │                                        │
│  │  oder JavaFX Desktop-App        │                                        │
│  │  - Port: HTTPS 443              │                                        │
│  │  - Läuft in: Chrome / Edge      │                                        │
│  └──────────────┬──────────────────┘                                        │
│                 │ HTTPS (REST/JSON)                                          │
│                 ▼                                                            │
│  ┌─────────────────────────────────┐                                        │
│  │  ANWENDUNGSSCHICHT              │                                        │
│  │  Spring Boot 3.x REST API       │                                        │
│  │  (Windows Service via WinSW)    │                                        │
│  │  - Port: HTTPS 8443             │                                        │
│  │  - Spring Security + JWT        │                                        │
│  │  - Prepared Statements only     │                                        │
│  │  - SLF4J + Logback Logging      │                                        │
│  │  - Flyway DB-Migrationen        │                                        │
│  └──────────────┬──────────────────┘                                        │
│                 │ JDBC/SSL:1433                                              │
│                 ▼                                                            │
│  ┌─────────────────────────────────┐                                        │
│  │  DATENSCHICHT                   │                                        │
│  │  MS SQL Server 2019/2022        │                                        │
│  │  (Standard/Developer Edition)   │                                        │
│  │  - Verschlüsselung: TDE         │                                        │
│  │  - SQL Agent: Auto-Backup       │                                        │
│  │  - Windows Authentication       │                                        │
│  └─────────────────────────────────┘                                        │
│                                                                             │
│  ┌─────────────────────────────────┐                                        │
│  │  BACKUP-SCHICHT                 │                                        │
│  │  SQL Server Backup Jobs         │                                        │
│  │  Windows Task Scheduler         │                                        │
│  │  Externer Backup-Speicher       │                                        │
│  └─────────────────────────────────┘                                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Komponentenübersicht

#### 2.3.1 Präsentationsschicht

**Technologie**: Angular 17+ (TypeScript) als Single-Page-Application  
**Betrieb**: Statische Dateien werden vom Spring Boot Backend ausgeliefert (kein separater Node-Server nötig)  
**Zugriff**: Beliebiger moderner Browser (Chrome, Edge) – **keine Client-Installation**

Hauptmodule der UI:
- **Login-Modul**: JWT-basierte Anmeldung
- **Mitglieder-Modul**: Verwaltung von Personen/Mitgliedern
- **Spenden-Modul**: Spendenerfassung und -verwaltung
- **Bußgeld-Modul**: Bußgeld- und Eingangsverwaltung
- **Report-Modul**: Excel-Export, Briefe, Serienbriefe
- **Admin-Modul**: Benutzerverwaltung, Stammdaten

#### 2.3.2 Anwendungsschicht

**Technologie**: Spring Boot 3.x (Java 17+), läuft als **Windows-Dienst** via WinSW

```
frauenhaus-backend/
├── src/main/java/de/frauenhaus/
│   ├── config/
│   │   ├── SecurityConfig.java       # Spring Security + JWT
│   │   ├── DataSourceConfig.java     # SQL Server DataSource
│   │   └── WebMvcConfig.java         # CORS, static files
│   ├── controller/
│   │   ├── AuthController.java       # POST /api/auth/login
│   │   ├── MitgliedController.java   # CRUD /api/mitglieder
│   │   ├── SpendeController.java     # CRUD /api/spenden
│   │   ├── BussgeldController.java   # CRUD /api/bussgelder
│   │   └── ReportController.java     # GET  /api/reports/*
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── MitgliedService.java
│   │   ├── SpendeService.java
│   │   ├── BussgeldService.java
│   │   └── ReportService.java        # POI 5.x Excel-Generierung
│   ├── repository/
│   │   ├── MitgliedRepository.java   # JPA/Hibernate
│   │   ├── SpendeRepository.java
│   │   └── BussgeldRepository.java
│   ├── entity/
│   │   ├── Mitglied.java
│   │   ├── Spende.java
│   │   ├── Bussgeld.java
│   │   ├── Eingang.java
│   │   └── User.java
│   └── security/
│       ├── JwtTokenProvider.java
│       └── UserDetailsServiceImpl.java
├── src/main/resources/
│   ├── application.yml               # Zentrale Konfiguration
│   ├── application-prod.yml          # Produktionskonfiguration
│   └── db/migration/                 # Flyway SQL-Skripte
│       ├── V1__initial_schema.sql
│       └── V2__add_user_roles.sql
└── frauenhaus-service.xml            # WinSW Service-Descriptor
```

#### 2.3.3 Datenschicht

**Technologie**: Microsoft SQL Server 2019/2022 Standard oder Developer Edition

Verbesserungen gegenüber IST:
- **Volle Edition** statt Express (kein 4-GB-Limit, SQL Server Agent verfügbar)
- **Transparent Data Encryption (TDE)**: Datenbankdateien auf Disk verschlüsselt
- **SSL/TLS**: Verschlüsselte JDBC-Verbindung
- **Windows Authentication**: Anwendung verbindet sich per Windows-Dienstkonto (kein Passwort in Config)
- **Contained Database**: Sichererer DB-Betrieb

---

## 3. Datensicherheit (Schwerpunkt)

### 3.1 Authentifizierung & Autorisierung

```
Client                Backend                  DB
  │                      │                      │
  │── POST /api/login ──►│                      │
  │   {user, password}   │── SELECT user ──────►│
  │                      │◄─ BCrypt-Hash ───────│
  │                      │ verify(input, hash)  │
  │◄── JWT Token ────────│                      │
  │                      │                      │
  │── GET /api/mitglieder│                      │
  │   Authorization:     │                      │
  │   Bearer <JWT>  ────►│ verify JWT signature │
  │                      │── SELECT * FROM ────►│
  │◄─ JSON Response ─────│◄─ ResultSet ─────────│
```

**Maßnahmen:**
| Maßnahme | Beschreibung |
|----------|-------------|
| **Passwort-Hashing** | BCrypt mit Kostenfaktor 12 – kein Klartext-Passwort |
| **JWT Authentication** | Stateless, signierte Tokens (HS256/RS256), 8h Ablauf |
| **Refresh Tokens** | Langlebige Refresh-Tokens im HTTP-only-Cookie |
| **HTTPS Only** | TLS 1.2/1.3 Pflicht – kein HTTP |
| **RBAC** | Rollen: `ADMIN`, `SACHBEARBEITER`, `VIEWER` |
| **Windows Auth** | Backend verbindet sich per Windows-Dienstkonto (kein DB-Passwort in Config) |

### 3.2 SQL-Injection-Schutz

**IST** (gefährlich):
```java
// VORHER - SQL Injection möglich!
String SQLString = "SELECT u.main_frame FROM compucrash.user_def u "
    + "WHERE LOWER(u.user_name) = LOWER('" + CPropertyManager.USER + "')";
```

**SOLL** (sicher):
```java
// NACHHER - JPA/Hibernate mit Prepared Statements
@Query("SELECT m FROM Mitglied m WHERE LOWER(m.name) = LOWER(:name)")
List<Mitglied> findByName(@Param("name") String name);
// ODER Spring Data Repository - automatisch sicher
Optional<Mitglied> findByNameIgnoreCase(String name);
```

### 3.3 Datenbankebene – TDE

```sql
-- Transparent Data Encryption aktivieren
USE master;
CREATE MASTER KEY ENCRYPTION BY PASSWORD = '<StarkePassphrase>';
CREATE CERTIFICATE FrauenhausCert WITH SUBJECT = 'Frauenhaus TDE Cert';
USE frauenhaus;
CREATE DATABASE ENCRYPTION KEY
    WITH ALGORITHM = AES_256
    ENCRYPTION BY SERVER CERTIFICATE FrauenhausCert;
ALTER DATABASE frauenhaus SET ENCRYPTION ON;
```

### 3.4 Netzwerksicherheit

```
┌─────────────────┐    HTTPS:443      ┌──────────────────────┐
│  Browser/Client  │ ─────────────── ► │  Nginx Reverse Proxy  │
│  (LAN intern)    │                  │  (optional, Windows)  │
└─────────────────┘                  └──────────┬───────────┘
                                                │ HTTP:8080 lokal
                                     ┌──────────▼───────────┐
                                     │  Spring Boot Backend  │
                                     │  (localhost only)     │
                                     └──────────┬───────────┘
                                                │ JDBC/SSL:1433
                                     ┌──────────▼───────────┐
                                     │  SQL Server           │
                                     │  (localhost only,     │
                                     │   TDE aktiv)          │
                                     └──────────────────────┘
```

**Firewall-Regeln:**
- Port 1433 (SQL Server): **nur localhost** – kein direkter Netzwerkzugriff
- Port 8080 (Backend): nur über Reverse-Proxy exponiert
- Port 443 (HTTPS): einziger öffentlicher Port im LAN

---

## 4. Backup & Recovery (Schwerpunkt)

### 4.1 Backup-Strategie (3-2-1-Regel)

```
3 Kopien der Daten:  1x Produktionsdatenbank + 2x Backup
2 verschiedene Medien: Lokale Festplatte + Netzlaufwerk/USB
1 offsite-Kopie:     Externes Laufwerk oder Cloud (OneDrive for Business)
```

### 4.2 SQL Server Agent Jobs

```sql
-- Job 1: Vollbackup täglich um 23:00 Uhr
EXEC sp_add_job @job_name = N'Frauenhaus_FullBackup_Daily';
EXEC sp_add_jobstep @job_name = N'Frauenhaus_FullBackup_Daily',
    @command = N'BACKUP DATABASE [frauenhaus]
    TO DISK = N''C:\Backup\frauenhaus_full_'' 
            + CONVERT(VARCHAR(8), GETDATE(), 112) + ''.bak''
    WITH COMPRESSION, CHECKSUM, STATS = 10;';
EXEC sp_add_schedule @schedule_name = N'Täglich 23:00',
    @freq_type = 4, @freq_interval = 1,
    @active_start_time = 230000;

-- Job 2: Differenzielles Backup alle 6 Stunden
EXEC sp_add_job @job_name = N'Frauenhaus_DiffBackup_6h';
EXEC sp_add_jobstep @job_name = N'Frauenhaus_DiffBackup_6h',
    @command = N'BACKUP DATABASE [frauenhaus]
    TO DISK = N''C:\Backup\frauenhaus_diff_'' 
            + CONVERT(VARCHAR(8), GETDATE(), 112) + ''_''
            + CONVERT(VARCHAR(6), GETDATE(), 108) + ''.bak''
    WITH DIFFERENTIAL, COMPRESSION;';

-- Job 3: Log-Backup jede Stunde (Recovery Model: FULL)
EXEC sp_add_job @job_name = N'Frauenhaus_LogBackup_1h';
EXEC sp_add_jobstep @job_name = N'Frauenhaus_LogBackup_1h',
    @command = N'BACKUP LOG [frauenhaus]
    TO DISK = N''C:\Backup\frauenhaus_log_'' 
            + CONVERT(VARCHAR(8), GETDATE(), 112) + ''_''
            + CONVERT(VARCHAR(6), GETDATE(), 108) + ''.trn'';';
```

### 4.3 Backup-Rotation (Windows Task Scheduler)

```powershell
# backup_cleanup.ps1 – täglich ausgeführt via Task Scheduler
# Vollbackups: 30 Tage aufbewahren
Get-ChildItem "C:\Backup\frauenhaus_full_*.bak" |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-30) } |
    Remove-Item -Force

# Differenzielle Backups: 7 Tage
Get-ChildItem "C:\Backup\frauenhaus_diff_*.bak" |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-7) } |
    Remove-Item -Force

# Log-Backups: 2 Tage
Get-ChildItem "C:\Backup\frauenhaus_log_*.trn" |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-2) } |
    Remove-Item -Force

# Backup auf Netzlaufwerk kopieren (offsiting)
$date = Get-Date -Format "yyyyMMdd"
Copy-Item "C:\Backup\frauenhaus_full_$date.bak" "\\fileserver\backup\frauenhaus\" -ErrorAction SilentlyContinue

Write-EventLog -LogName Application -Source "FrauenhausBackup" `
    -EventId 1000 -EntryType Information `
    -Message "Backup-Cleanup erfolgreich abgeschlossen"
```

### 4.4 Backup-Überprüfung (RESTORE VERIFYONLY)

```sql
-- Wöchentlicher Verifizierungsjob
RESTORE VERIFYONLY
    FROM DISK = N'C:\Backup\frauenhaus_full_' 
              + CONVERT(VARCHAR(8), GETDATE()-1, 112) + '.bak'
    WITH CHECKSUM;
-- Ergebnis wird in msdb.dbo.backupset protokolliert
```

### 4.5 Recovery-Prozedur (dokumentiert)

| Szenario | Maßnahme | RTO (Ziel) |
|----------|----------|------------|
| Versehentlich gelöschter Datensatz | Point-in-Time-Recovery via Log-Backup | < 30 min |
| DB-Korruption | Vollbackup + Diff + Logs einspielen | < 2h |
| Serverausfall | Backup auf neuem System einspielen | < 4h |
| Totalverlust (offsite) | Backup vom Netzlaufwerk/extern | < 8h |

**Schritt-für-Schritt Recovery:**
```sql
-- 1. Vollbackup einspielen (WITH NORECOVERY = weitere Backups folgen)
RESTORE DATABASE frauenhaus
    FROM DISK = 'C:\Backup\frauenhaus_full_20240115.bak'
    WITH NORECOVERY, REPLACE;

-- 2. Differenzielles Backup einspielen
RESTORE DATABASE frauenhaus
    FROM DISK = 'C:\Backup\frauenhaus_diff_20240115_180000.bak'
    WITH NORECOVERY;

-- 3. Log-Backups einspielen bis zum gewünschten Zeitpunkt
RESTORE LOG frauenhaus
    FROM DISK = 'C:\Backup\frauenhaus_log_20240115_190000.trn'
    WITH RECOVERY, STOPAT = '2024-01-15 19:45:00';
-- RECOVERY aktiviert die Datenbank
```

---

## 5. Einfache Wartung (Schwerpunkt)

### 5.1 Zentrale Konfiguration

**IST**: Pro-PC-Konfigurationsdateien (.ini) → mühsam bei Änderungen

**SOLL**: Einheitliche `application.yml` auf dem Server:

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=frauenhaus;encrypt=true;integratedSecurity=true
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8443
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12

logging:
  level:
    de.frauenhaus: INFO
    org.springframework.security: WARN
  file:
    name: C:/Logs/frauenhaus/application.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 30

frauenhaus:
  reports:
    output-dir: C:/frauenhaus/reports
  templates:
    dir: C:/frauenhaus/vorlagen
  email:
    address: frauenhaus-mannheim@t-online.de
```

### 5.2 Windows-Dienst (WinSW)

Die Anwendung läuft als **Windows-Dienst** – automatischer Start, Neustart bei Fehler:

```xml
<!-- frauenhaus-service.xml -->
<service>
  <id>FrauenhausBackend</id>
  <name>Frauenhaus Verwaltung Backend</name>
  <description>Frauenhaus Adress- und Bußgeldverwaltung REST-Backend</description>
  <executable>java</executable>
  <arguments>-jar C:\frauenhaus\frauenhaus-backend.jar --spring.profiles.active=prod</arguments>
  <logpath>C:\Logs\frauenhaus</logpath>
  <log mode="roll-by-size">
    <sizeThreshold>10240</sizeThreshold>
    <keepFiles>10</keepFiles>
  </log>
  <onfailure action="restart" delay="10 sec"/>
  <onfailure action="restart" delay="20 sec"/>
  <onfailure action="none"/>
  <startmode>Automatic</startmode>
  <serviceaccount>
    <domain>.</domain>
    <user>frauenhaus-svc</user>
    <allowservicelogon>true</allowservicelogon>
  </serviceaccount>
</service>
```

**Dienst-Verwaltung:**
```cmd
:: Installation
frauenhaus-service.exe install

:: Start/Stop/Status
net start FrauenhausBackend
net stop FrauenhausBackend
sc query FrauenhausBackend
```

### 5.3 Datenbank-Migrationen (Flyway)

Schema-Änderungen werden **versioniert und automatisch** eingespielt:

```
db/migration/
├── V1__initial_schema.sql          # Initiales Schema (Migration aus Access/SQL Server)
├── V2__add_user_roles.sql          # RBAC-Tabellen
├── V3__add_audit_log.sql           # Audit-Log-Tabelle
├── V4__add_indexes.sql             # Performance-Indizes
└── V5__encrypt_sensitive_fields.sql # Felder-Verschlüsselung
```

```sql
-- V3__add_audit_log.sql
CREATE TABLE frauenhaus.audit_log (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    zeitpunkt    DATETIME2 NOT NULL DEFAULT GETDATE(),
    benutzer     NVARCHAR(100) NOT NULL,
    aktion       NVARCHAR(50) NOT NULL,  -- INSERT, UPDATE, DELETE
    tabelle      NVARCHAR(100) NOT NULL,
    datensatz_id NVARCHAR(100),
    alt_wert     NVARCHAR(MAX),
    neu_wert     NVARCHAR(MAX)
);
```

### 5.4 Update-Prozess

```
┌──────────────────────────────────────────────────────────┐
│  Update-Prozedur (< 10 Minuten)                         │
│                                                          │
│  1. Neues frauenhaus-backend-x.y.z.jar bereitstellen    │
│  2. net stop FrauenhausBackend                           │
│  3. Altes JAR sichern (rename)                           │
│  4. Neues JAR kopieren                                   │
│  5. net start FrauenhausBackend                          │
│     → Flyway spielt DB-Migrationen automatisch ein       │
│  6. Browser-Refresh auf Client – fertig                  │
│                                                          │
│  Rollback: Altes JAR zurückkopieren, Dienst neu starten  │
└──────────────────────────────────────────────────────────┘
```

### 5.5 Monitoring & Alerting

```yaml
# Spring Boot Actuator (Health Checks)
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      show-details: when-authorized
```

- **Health-Endpoint**: `https://localhost:8443/actuator/health`  
- **Windows Event Log**: Kritische Fehler werden ins Windows Event Log geschrieben
- **Disk-Monitoring**: PowerShell-Skript prüft täglich Backup-Verzeichnis und Festplattenplatz

---

## 6. Vergleich IST vs. SOLL

| Kriterium               | IST                                  | SOLL                                         |
|-------------------------|--------------------------------------|----------------------------------------------|
| **Architektur**         | 2-Schichten (Fat Client + DB)        | 3-Schichten (Browser + Backend + DB)         |
| **Client-Installation** | Java + App auf jedem PC              | Nur Browser nötig                            |
| **Passwörter**          | Klartext in .ini-Dateien             | BCrypt gehasht, DB via Windows-Auth          |
| **SQL-Injection**       | Anfällig (String-Konkatenation)      | Sicher (JPA Prepared Statements)             |
| **Transport**           | Unverschlüsselt (JDBC TCP)           | TLS 1.3 Ende-zu-Ende                         |
| **DB-Verschlüsselung**  | Keine                                | TDE (AES 256)                                |
| **Backup**              | Manuell / nicht dokumentiert         | Automatisch, 3 Ebenen (Full/Diff/Log)        |
| **Recovery**            | Nicht dokumentiert                   | Dokumentiert, getestet, RTO definiert        |
| **Konfiguration**       | Pro PC (.ini-Dateien)                | Zentral (application.yml auf Server)         |
| **Updates**             | Manuelle Neuinstallation pro PC      | JAR-Austausch + Dienst-Neustart (< 10 min)  |
| **Logging**             | System.out.println()                 | SLF4J + Logback + Windows Event Log          |
| **DB-Schema-Änderungen**| Manuell, undokumentiert              | Flyway, versioniert, automatisch             |
| **Fehlerbehandlung**    | System.exit(0) bei Fehler            | Exception-Handler, HTTP-Fehlercodes          |
| **Rollenkonzept**       | Nur in DB-Tabellen                   | Spring Security RBAC + JWT                   |
| **Audit-Log**           | Keiner                               | Vollständiger Audit-Trail in DB              |

---

## 7. Technologie-Stack (SOLL)

| Komponente          | Technologie                              | Version      |
|---------------------|------------------------------------------|--------------|
| **Laufzeit**        | Java (OpenJDK / Temurin)                 | 17 LTS       |
| **Backend-Framework** | Spring Boot                            | 3.2.x        |
| **Sicherheit**      | Spring Security + JWT (JJWT)             | 6.x          |
| **Daten-Zugriff**   | Spring Data JPA + Hibernate              | 6.x          |
| **DB-Treiber**      | Microsoft JDBC Driver for SQL Server     | 12.x         |
| **DB-Migration**    | Flyway                                   | 10.x         |
| **Datenbank**       | MS SQL Server                            | 2019/2022    |
| **Reporting**       | Apache POI                               | 5.x          |
| **Frontend**        | Angular                                  | 17+          |
| **Dienstverwaltung**| WinSW (Windows Service Wrapper)          | 2.x          |
| **Build**           | Maven                                    | 3.9.x        |
| **Logging**         | SLF4J + Logback                          | 2.x          |
| **Tests**           | JUnit 5 + Mockito + AssertJ              | 5.x          |

---

## 8. Migrations-Roadmap

### Phase 1 – Datenbankebene (Woche 1–2)
- [ ] SQL Server Express → SQL Server Standard/Developer migrieren
- [ ] TDE aktivieren
- [ ] Recovery Model auf FULL umstellen
- [ ] SQL Agent Backup-Jobs einrichten
- [ ] Backup-Verifikation einrichten
- [ ] Benutzerkonten und Rollen neu strukturieren

### Phase 2 – Backend (Woche 3–6)
- [ ] Spring Boot Projekt aufsetzen (Maven)
- [ ] Datenbankentitäten (JPA) erstellen
- [ ] Flyway Migrationen für bestehendes Schema
- [ ] REST-Endpunkte implementieren (CRUD für alle Entitäten)
- [ ] Spring Security + JWT konfigurieren
- [ ] Windows-Auth für DB-Verbindung
- [ ] Report-Service (POI 5.x, Excel)
- [ ] Brief-Generierung (Apache POI XWPF für .docx)
- [ ] Als Windows-Dienst einrichten (WinSW)
- [ ] HTTPS konfigurieren

### Phase 3 – Frontend (Woche 5–10)
- [ ] Angular Projekt aufsetzen
- [ ] Login-Modul
- [ ] Mitglieder-Verwaltung
- [ ] Spenden-Verwaltung
- [ ] Bußgeld-Verwaltung
- [ ] Report-Downloads
- [ ] Admin-Bereich (Benutzerverwaltung, Stammdaten)

### Phase 4 – Testen & Abnahme (Woche 11–12)
- [ ] Unit Tests (JUnit 5 + Mockito) für alle Services
- [ ] Integration Tests
- [ ] Backup & Recovery Prozedur testen (RESTORE VERIFYONLY)
- [ ] Penetrationstest (SQL-Injection, XSS, Auth)
- [ ] Nutzer-Akzeptanztests (UAT) mit tatsächlichen Nutzern
- [ ] Dokumentation

### Phase 5 – Go-Live & Schulung (Woche 13)
- [ ] Datenmigration vom alten System
- [ ] Parallelbet rieb (2 Wochen)
- [ ] Nutzer-Schulung
- [ ] Abschalten des alten Systems

---

## 9. Metriken – Anwendung auf die neue Architektur

Die folgenden Software-Metriken sollen auf die neue Architektur (SOLL) angewandt werden,
analog zur IST-Zustand-Analyse der Metriken-Gruppe:

### 9.1 Komplexitätsmetriken

| Metrik | IST (Schätzung) | SOLL (Ziel) | Werkzeug |
|--------|-----------------|-------------|---------|
| **Zyklomatische Komplexität** | Hoch (SQL-String-Building) | Niedrig (JPA Queries) | SonarQube, PMD |
| **Lines of Code (LoC)** | ~15.000 | ~20.000 (inkl. Tests) | SonarQube |
| **Test Coverage** | ~0% | ≥ 80% | JaCoCo |
| **Code Duplication** | Hoch (Copy-Paste) | < 5% | SonarQube |

### 9.2 Kopplungsmetriken

| Metrik | IST | SOLL |
|--------|-----|------|
| **Afferente Kopplung (Ca)** | Hoch (globale Singletons) | Niedrig (DI via Spring) |
| **Efferente Kopplung (Ce)** | Hoch (direkte DB-Aufrufe) | Repository-Pattern |
| **Instability (I = Ce/(Ca+Ce))** | Hoch | Gering bei Service-Layer |

### 9.3 Sicherheitsmetriken

| Metrik | IST | SOLL |
|--------|-----|------|
| **OWASP Top 10 Verstöße** | SQL Injection, Broken Auth | Keine bekannten |
| **CVE-Risiko** | Hoch (POI 2004, jTDS 2004) | Niedrig (aktuelle Versionen) |
| **CVSS-Score (Abhängigkeiten)** | > 7.0 (kritisch) | < 4.0 (niedrig) |

### 9.4 Wartbarkeitsmetriken (nach ISO 25010)

| Merkmal | IST | SOLL |
|---------|-----|------|
| **Analysierbarkeit** | Schlecht (kein Logging) | Gut (Logback, Actuator) |
| **Modifizierbarkeit** | Schlecht (keine Tests) | Gut (JUnit 5, 80% Coverage) |
| **Testbarkeit** | Schlecht (alles verwoben) | Gut (klare Schichten, DI) |
| **Wiederverwendbarkeit** | Schlecht (monolithisch) | Gut (REST API) |

---

*Dokument erstellt: Juni 2026 | FH Masterprojekt – ASE*
*System: Frauenhaus Adress- und Bußgeldverwaltung – Neue Architektur*
