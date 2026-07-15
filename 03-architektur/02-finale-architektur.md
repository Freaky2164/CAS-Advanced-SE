# Finale Architektur

Dieses Dokument fasst die finale Zielarchitektur zusammen, wie sie sich aus den einzelnen
Architecture Decision Records (ADRs) ergibt. Es dient als konsolidierter Überblick; die
Detailbegründungen, Alternativenbewertungen und Konsequenzen sind in den jeweiligen ADRs
nachzulesen:

| ADR | Thema | Entscheidung |
|-----|-------|--------------|
| [ADR-001](adrs/001-architecture-style.md) | Architekturstil | 3-Schichten-Architektur statt Fat-Client |
| [ADR-002](adrs/002-tech-stack-backend.md) | Tech-Stack Backend | Spring Boot 3.4.x / Java 21 LTS |
| [ADR-003](adrs/003-tech-stack-frontend.md) | Tech-Stack Frontend | Vaadin (Full-Stack Java) |
| [ADR-004](adrs/004-authentication-rbac.md) | Authentifizierung & Autorisierung | Zustandslose HTTP-Basic-Auth mit BCrypt und RBAC |
| [ADR-005](adrs/005-backup-strategy.md) | Backup-Strategie | pgBackRest mit WAL-Archivierung, 3-2-1-Regel |
| [ADR-006](adrs/006-deployment-model.md) | Deployment-Modell | On-Premises statt Cloud (DSGVO) |

---

## 1. Architekturüberblick

Die neue Anwendung ersetzt die bisherige Java-Swing-Desktop-Anwendung mit direktem
JDBC-Zugriff auf die Datenbank durch eine **3-Schichten-Architektur** (ADR-001):

```
Browser (Vaadin Web UI)  --> HTTPS -->  Backend (Spring Boot, Windows-Dienst)  --> JDBC -->  PostgreSQL
```

1. **Präsentationsschicht**: Web-Oberfläche mit Vaadin, direkt im Java-Backend gerendert
2. **Anwendungsschicht**: Zentrales Backend (Spring Boot) als Windows-Dienst, das
   Geschäftslogik, Validierung, Authentifizierung/Autorisierung und Datenzugriff kapselt
3. **Datenschicht**: Zentrale PostgreSQL-Datenbank auf demselben Server

Damit wird die zentrale Schwachstelle des IST-Systems – verteilte Clients mit direktem
Datenbankzugriff und Klartext-Zugangsdaten – vollständig eliminiert. Alle Zugriffe laufen
über eine einzige, zentral kontrollierte Anwendungsschicht.

## 2. Technologie-Stack

| Schicht | Technologie | Begründung (siehe ADR) |
|---------|-------------|--------------------------|
| Frontend | **Vaadin** (Full-Stack Java) | Enge Java-Integration, Wiederverwendung von Backend-Komponenten, umfangreiche UI-Komponenten "out of the box", geringerer Boilerplate-Code als Angular/React/Next.js (ADR-003) |
| Backend | **Spring Boot 3.4.x / Java 21 LTS** | Bestes Office-Dokumenten-Ökosystem (Apache POI, XDocReport), ausgereifte PostgreSQL-Integration, vorhandene Teamkompetenz aus dem Legacy-Java-System, LTS-Support bis 2031+ (ADR-002) |
| Datenbank | **PostgreSQL** | Lizenzkostenfrei, keine Größenlimitierung, erstklassige Spring-Data-JPA-Integration, kein Vendor-Lock-in (ADR-002) |
| Authentifizierung | **HTTP-Basic-Auth über HTTPS + BCrypt + RBAC** (Rollen `ADMIN`, `SACHBEARBEITUNG`) | Verhältnismäßig zum Bedrohungskontext (LAN, wenige bekannte Nutzer), zustandslos, minimaler Implementierungsaufwand (ADR-004) |
| Backup | **pgBackRest** mit WAL-Archivierung | Automatisiert, verschlüsselt (AES-256), Point-in-Time-Recovery, 3-2-1-Regel ohne Cloud-Abhängigkeit (ADR-005) |
| Deployment | **On-Premises** auf dediziertem Windows-Server, Backend als Windows-Dienst (WinSW) | Volle Datensouveränität, DSGVO-Konformität ohne Auftragsverarbeitung/Drittlandtransfer, keine laufenden Cloud-Kosten, minimale Angriffsfläche (ADR-006) |

## 3. Komponentenübersicht

```
Vereinsbüro - physisch geschützter Raum (ADR-006)

  Windows-Server
  +-----------------------------------+
  | Vaadin Web UI                     |
  | (Grid, Form, Dialog, ...)         |
  +-----------------+-----------------+
                    | direkter Java-Aufruf (kein REST nötig)
  +-----------------v-----------------+
  | Spring Boot (WinSW-Dienst)        |
  | Services, Business-Logik,         |
  | Spring Security (Basic-Auth,      |
  | BCrypt, RBAC)                     |
  +-----------------+-----------------+
                    | JDBC
  +-----------------v-----------------+     +----------------------+
  | PostgreSQL                        |---->| pgBackRest           |
  | Stammdaten, Spenden, Bußgelder,   |     | repo1: lokal (AES)   |
  | Dokumente (bytea)                 |     | repo2: USB extern    |
  +------------------------------------+     +----------------------+

                    ^ LAN (kein Internet-Zugang nötig)
                    |
               +---------+
               | Clients | (Browser im LAN)
               +---------+
```

## 4. Authentifizierung & Autorisierung

Gemäß ADR-004 authentifizieren sich Benutzer über **HTTP-Basic-Auth ausschließlich über
HTTPS**. Passwörter werden mit **BCrypt** gehasht (adaptiv, gesalzen) und ersetzen die im
IST-System vorhandenen Klartext-Zugangsdaten. Die Autorisierung erfolgt zustandslos
(`SessionCreationPolicy.STATELESS`) über ein einfaches RBAC-Modell mit genau zwei Rollen:

- **ADMIN**: voller Zugriff, inkl. Stammdatenverwaltung (Spendenarten, Anreden,
  Bußgeldstatus, Vereine, Gerichte) gemäß FR-7
- **SACHBEARBEITUNG**: operative Verwaltung von Mitgliedern, Spenden, Bußgeldern und
  Zahlungseingängen gemäß FR-2 bis FR-5

Ein initialer Administrator-Account wird beim ersten Start automatisiert angelegt
(`AdminBootstrap`), ohne ein hart codiertes Standardpasswort zu verwenden.

## 5. Backup & Wiederherstellung

Gemäß ADR-005 wird die PostgreSQL-Datenbank automatisiert über **pgBackRest** gesichert:

- **Vollbackup** wöchentlich (sonntags), **differenzielle Backups** an den übrigen Tagen
- **Kontinuierliche WAL-Archivierung** ermöglicht Point-in-Time-Recovery
- **AES-256-Verschlüsselung** der Backups, Passphrase getrennt verwahrt
- **3-2-1-Regel**: Produktivdatenbank, lokales verschlüsseltes Backup-Volume (`repo1`) und
  eine externe Kopie auf rotierenden, verschlüsselten USB-Datenträgern (`repo2`) an einem
  zweiten Standort – ohne Cloud-Abhängigkeit
- **Quartalsweiser Pflicht-Restore-Test** in einer Staging-Umgebung zur Verifikation der
  Wiederherstellbarkeit

Da Dokumente (z. B. Spendenbescheinigungen, Serienbriefe) als `bytea` direkt in PostgreSQL
gespeichert werden, deckt dieser eine Backup-Mechanismus sowohl Stammdaten als auch
Dokumente ab.

## 6. Deployment-Modell

Gemäß ADR-006 wird das Gesamtsystem **On-Premises** auf einem dedizierten Windows-Server im
lokalen Netzwerk des Vereins betrieben:

- Backend läuft als **Windows-Dienst** (WinSW), automatischer Start und Neustart bei Crash
- **Kein Internetzugang im Regelbetrieb** notwendig – Zugriff ausschließlich über das LAN
- Keine öffentliche IP-Adresse, kein offener Port nach außen → minimale Angriffsfläche
- Keine laufenden Cloud-Kosten; einmalige Hardwarekosten (Mini-PC/Server, USV)
- Volle Datenhoheit und DSGVO-Konformität ohne Auftragsverarbeitungsvertrag oder
  Drittlandtransfer-Problematik

## 7. Zuordnung zu den Qualitätsmerkmalen

| Qualitätsmerkmal (aus 01-requirements.md) | Umsetzung in der finalen Architektur |
|---------------------------------------------|----------------------------------------|
| **Funktionale Eignung (Korrektheit)** | Zentrale Validierung und Geschäftslogik im Spring-Boot-Backend, konsistente Datenhaltung in PostgreSQL |
| **Benutzerfreundlichkeit** | Vaadin liefert vorgefertigte, konsistente UI-Komponenten (Formulare, Tabellen, Dialoge) für nicht-technische Benutzer |
| **Sicherheit (DSGVO)** | On-Premises-Betrieb (ADR-006), BCrypt + RBAC (ADR-004), verschlüsselte Backups (ADR-005) |
| **Zuverlässigkeit** | Windows-Dienst mit automatischem Neustart, USV, automatisierte Backups mit Point-in-Time-Recovery |
| **Wartbarkeit** | 3-Schichten-Architektur mit zentralem Backend, Fat-JAR-Deployment (JAR austauschen + Dienst neustarten), Flyway-Migrationen, zentrales Logging |

## 8. Offene Punkte / Ausblick

- Konkrete Cron-Zeitpläne und Retention-Werte für Backups können bei Bedarf angepasst werden
  (ADR-005)
- Eine spätere externe Erreichbarkeit (Abweichung von ADR-006) würde eine Neubewertung der
  Authentifizierungsstrategie (ADR-004, z. B. JWT) erfordern
- Passwort-Policy (Mindestlänge, Komplexität) ist organisatorisch zu ergänzen, kein
  architektonischer Bestandteil der bestehenden ADRs
