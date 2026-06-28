# ADR-002: Technologie-Stack Backend – Spring Boot (Java 17)

## Status

**Akzeptiert** – Juni 2026

## Kontext

Gemäß ADR-001 wird eine 3-Schichten-Architektur mit zentralem REST-Backend implementiert.
Für die Anwendungsschicht muss eine Technologie gewählt werden, die folgende Anforderungen erfüllt:

| Anforderung                      | Beschreibung                                                                                                 |
|----------------------------------|--------------------------------------------------------------------------------------------------------------|
| **Office-Dokumentengenerierung** | Excel-Reports (Mitgliederlisten, Spendenübersichten) und Word-Briefe (.docx Serienbriefe, Spendenquittungen) |
| **PostgresSQL Anbingung**        | Neue Datenbank erstellen, mit Einbau von Sicherheitsbezogenen Aspekten                                       |
| **Sicherheit**                   | JWT-Authentifizierung, RBAC, Passwort-Hashing, HTTPS                                                         |
| **Windows-Dienst**               | Betrieb als automatisch startender Service (eventuell Docker?) ohne manuelle Interaktion                     |
| **Wartbarkeit**                  | Automatische DB-Migrationen, zentrales Logging, Health Checks                                                |
| **Teamkompetenz**                | Das Legacy-System ist in Java geschrieben – Java-Grundkenntnisse vorhanden                                   |
| **Langlebigkeit**                | Für einen Verein muss die Technologie über 5–10 Jahre stabil und gewartet bleiben                            |

## Begründung

### 1. Ausgereiftestes Office-Ökosystem

Apache POI ist die **de facto Standardbibliothek** für Office-Dokumentengenerierung in der
JVM-Welt (seit 2001, aktiv gepflegt). In Kombination mit docx4j oder XDocReport können
alle Anforderungen des Legacy-Systems erfüllt werden:

| Legacy-Funktion | Neue Lösung |
|-----------------|-------------|
| Excel-Reports (Mitgliederlisten, Spenden) | Apache POI XSSF/SXSSF |
| Word-Briefe (.dot-Vorlagen) | XDocReport + Freemarker Templates (.docx) |
| Serienbriefe | XDocReport Batch-Generierung |
| PDF-Export | Apache PDFBox oder POI → PDF-Konvertierung |

Keine andere Sprache bietet ein gleichwertiges Open-Source-Ökosystem für diese Kombination.

### 2. Nahtlose SQL Server Integration

Der Microsoft JDBC Driver for SQL Server (aktiv von Microsoft gepflegt) unterstützt:
- Windows Integrated Authentication (`integratedSecurity=true`) – kein DB-Passwort in der Konfiguration
- TLS-verschlüsselte Verbindungen (`encrypt=true`)
- Always Encrypted, Azure AD Auth (für spätere Cloud-Migration)

Spring Data JPA eliminiert SQL-Injection **strukturell** durch Prepared Statements.

### 3. Vorhandene Teamkompetenz

Das Legacy-System ist in Java geschrieben. Das Team kann:
- Bestehende Geschäftslogik (Berechnungen, Validierungen) verstehen und portieren
- Java-Syntax und -Semantik ohne Einarbeitungszeit nutzen
- Von Java 1.5 auf Java 25 aufbauen (gleiche Sprache, modernisierte Features)

Ein Wechsel zu C# oder Python würde einen kompletten Rewrite erfordern und wurde von uns als unrealistisch für den zeitlichen Kontext erachtet.

### 4. Convention over Configuration

Spring Boot minimiert Konfigurationsaufwand durch Auto-Configuration:
- Embedded Tomcat (kein separater Application Server)
- Auto-konfigurierte DataSource, JPA, Security
- Fat-JAR-Packaging: **eine Datei** = gesamte Anwendung
- Spring Initializr für Projektsetup in Minuten

### 5. Langfristige Stabilität




### 6. Windows-Dienst-Betrieb

Containerisierung erweist sich in der Industrie nicht erst seit kurzer Zeit als eine zuverlässige Methode der Bereiststellung von Services und Anwendungen.

### Positiv
- Direkter Wissenstransfer vom Legacy-Java-Code möglich
- Bestmögliche Unterstützung für Office-Dokumentengenerierung (POI, XDocReport)
- Spring Security löst alle identifizierten Sicherheitsprobleme des IST-Systems
- Fat-JAR-Deployment vereinfacht Updates auf ein Minimum (JAR austauschen, Dienst neustarten)
- Flyway-Integration automatisiert Datenbankmigrationen
- Actuator-Endpoints ermöglichen Monitoring ohne zusätzliche Tools

### Negativ
- JVM-Startup ist langsamer als native Anwendungen (~3–5 Sekunden) – für einen
  Windows-Dienst irrelevant, da nur einmal gestartet
- Höherer Speicherverbrauch als Python/Go (~200–400 MB) – akzeptabel auf dediziertem Server
- Spring Boot hat steile Lernkurve bei fortgeschrittenen Features (Security-Konfiguration) –
  mitigiert durch exzellente Dokumentation und Community

### Neutral
- Frontend-Technologie (Angular) muss separat entschieden werden (unabhängig vom Backend-Stack)
