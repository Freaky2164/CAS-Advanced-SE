# ADR-002: Technologie-Stack Backend – Spring Boot (Java 17)

## Status

**Akzeptiert** – Juni 2026

## Kontext

Gemäß ADR-001 wird eine 3-Schichten-Architektur mit zentralem REST-Backend implementiert.
Für die Anwendungsschicht muss eine Technologie gewählt werden, die folgende Anforderungen erfüllt:

| Anforderung | Beschreibung |
|-------------|-------------|
| **Office-Dokumentengenerierung** | Excel-Reports (Mitgliederlisten, Spendenübersichten) und Word-Briefe (.docx Serienbriefe, Spendenquittungen) |
| **MS SQL Server Anbindung** | Bestehende Datenbank weiternutzen, idealerweise mit Windows Authentication |
| **Sicherheit** | JWT-Authentifizierung, RBAC, Passwort-Hashing, HTTPS |
| **Windows-Dienst** | Betrieb als automatisch startender Windows Service ohne manuelle Interaktion |
| **Wartbarkeit** | Automatische DB-Migrationen, zentrales Logging, Health Checks |
| **Teamkompetenz** | Das Legacy-System ist in Java geschrieben – Java-Grundkenntnisse vorhanden |
| **Langlebigkeit** | Für einen Verein muss die Technologie über 5–10 Jahre stabil und gewartet bleiben |

## Entscheidung

Wir entscheiden uns für **Spring Boot 3.2.x mit Java 17 LTS** (OpenJDK / Eclipse Temurin)
als Backend-Technologie, deployed als Fat-JAR und betrieben als Windows-Dienst via WinSW.

## Betrachtete Alternativen

### Alternative A: C# / ASP.NET Core (.NET 8)

| Aspekt | Bewertung |
|--------|-----------|
| Office-Dokumente | ✅ OpenXML SDK (Microsoft-nativ), NPOI (Apache-POI-Port) |
| SQL Server | ✅ Erstklassig – Entity Framework Core, native Windows Auth |
| Windows-Integration | ✅ Exzellent – native Windows Services, COM-Interop für Outlook |
| Sicherheit | ✅ ASP.NET Identity, JWT Bearer, Data Protection API |
| Performance | ✅ Sehr gut (AOT-Kompilierung, Kestrel) |
| Ökosystem | ✅ NuGet, ausgereifte Tooling-Chain (Visual Studio) |
| Teamkompetenz | ❌ Keine C#-Erfahrung im Team vorhanden |
| Migration | ❌ Legacy-Code (Java) kann nicht wiederverwendet werden |
| Lizenzkosten | ⚠️ Visual Studio Professional kostenpflichtig (Community Edition limitiert) |
| Langlebigkeit | ✅ .NET LTS-Releases (3 Jahre Support) |

**Bewertung**: Technisch gleichwertig oder in Teilbereichen (Windows-Integration) sogar leicht
überlegen. Jedoch erfordert C# eine komplette Neueinarbeitung des Teams. Der bestehende
Java-Code (Geschäftslogik, Berechnungen) kann nicht portiert werden, ohne ihn vollständig
neu zu schreiben. Für ein Hochschulprojekt mit begrenzter Zeit ist der Technologiewechsel
ein unnötiges Risiko.

### Alternative B: Python / Django (oder FastAPI)

| Aspekt | Bewertung |
|--------|-----------|
| Office-Dokumente | ⚠️ python-docx (begrenzt, keine Serienbriefe nativ), openpyxl (Excel OK) |
| SQL Server | ⚠️ pyodbc funktioniert, aber Windows Auth erfordert zusätzliche Konfiguration |
| Windows-Integration | ❌ Kein nativer Windows-Dienst, erfordert NSSM oder ähnliche Wrapper |
| Sicherheit | ✅ Django: batteries-included (Auth, CSRF, ORM), FastAPI: manuell |
| Performance | ⚠️ GIL limitiert Concurrency, für wenige Nutzer aber ausreichend |
| Ökosystem | ✅ pip/PyPI, sehr schnelle Prototypen-Entwicklung |
| Teamkompetenz | ⚠️ Grundkenntnisse, aber keine Produktionserfahrung |
| Office-Qualität | ❌ python-docx unterstützt keine .dot/.dotx Templates nativ, keine Mail Merge |
| Typsicherheit | ❌ Dynamisch typisiert – Fehler erst zur Laufzeit, schwieriger Refactoring |
| Langlebigkeit | ⚠️ Django stabil, aber Python-Versioning (2→3 Trauma) und schnellere Breaking Changes |

**Bewertung**: Python eignet sich hervorragend für Scripting und Data Science, aber die
Office-Dokumentengenerierung ist deutlich schwächer als in Java/C#. `python-docx` unterstützt
keine Template-basierte Serienbriefgenerierung und keine komplexe Formatierung. Zudem fehlt
die robuste Windows-Dienst-Integration. Für eine langlebige Vereinsanwendung ist die
dynamische Typisierung ein Wartbarkeitsrisiko.

### Alternative C: Spring Boot 3.x / Java 17 (gewählt) ✅

| Aspekt | Bewertung |
|--------|-----------|
| Office-Dokumente | ✅ Apache POI 5.x (Excel/Word), docx4j (komplexe .docx), XDocReport (Serienbriefe mit Freemarker/Velocity) |
| SQL Server | ✅ Microsoft JDBC Driver, Windows Auth via integratedSecurity=true |
| Windows-Integration | ✅ WinSW als Windows Service, stabil seit Jahren im Einsatz |
| Sicherheit | ✅ Spring Security 6 (JWT, BCrypt, RBAC, CSRF, CORS), produktionserprobt |
| Performance | ✅ JVM-Performance für I/O-lastige Anwendungen hervorragend |
| Ökosystem | ✅ Maven Central, größtes Java-Ökosystem, Spring Initializr |
| Teamkompetenz | ✅ Java-Grundkenntnisse aus Legacy-System vorhanden |
| Migration | ✅ Geschäftslogik aus altem Java-Code teilweise übernehmbar |
| Typsicherheit | ✅ Statisch typisiert – Compile-Time-Fehler, sicheres Refactoring |
| Langlebigkeit | ✅ Java 17 LTS (Support bis 2029+), Spring Boot kommerzielle Support-Optionen |
| DB-Migrationen | ✅ Flyway nativ integriert |
| Monitoring | ✅ Spring Boot Actuator (Health, Metrics, Info) out-of-the-box |
| Testing | ✅ JUnit 5 + Mockito + @SpringBootTest – schichtweise testbar |

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
- Von Java 1.4 auf Java 17 aufbauen (gleiche Sprache, modernisierte Features)

Ein Wechsel zu C# oder Python würde eine komplette Spracheinarbeitung erfordern –
unverhältnismäßig für ein zeitlich begrenztes Hochschulprojekt.

### 4. Convention over Configuration

Spring Boot minimiert Konfigurationsaufwand durch Auto-Configuration:
- Embedded Tomcat (kein separater Application Server)
- Auto-konfigurierte DataSource, JPA, Security
- Fat-JAR-Packaging: **eine Datei** = gesamte Anwendung
- Spring Initializr für Projektsetup in Minuten

### 5. Langfristige Stabilität

| Kriterium | Java 17 / Spring Boot |
|-----------|----------------------|
| LTS-Support | Java 17: bis mind. September 2029 (Temurin) |
| Framework-Reife | Spring Framework: seit 2003, Spring Boot: seit 2014 |
| Abwärtskompatibilität | Java ist bekannt für strenge Rückwärtskompatibilität |
| Community | >10 Mio. Java-Entwickler weltweit (TIOBE #1–3 seit 25 Jahren) |
| Nachbesetzung | Einfach qualifizierte Entwickler zu finden |

### 6. Windows-Dienst-Betrieb

WinSW (Windows Service Wrapper) ist eine bewährte Lösung, um Java-Anwendungen als
Windows-Dienste zu betreiben:
- Automatischer Start bei Systemboot
- Automatischer Neustart bei Crash
- Service-Account (kein interaktiver Login nötig)
- Triviale Installation (`frauenhaus-service.exe install`)

## Entscheidungsmatrix (gewichtete Bewertung)

| Kriterium (Gewicht) | Spring Boot/Java 17 | ASP.NET Core/C# | Django/Python |
|---------------------|:-------------------:|:----------------:|:-------------:|
| Office-Dokumente (25%) | ★★★★★ | ★★★★☆ | ★★☆☆☆ |
| Teamkompetenz (20%) | ★★★★★ | ★★☆☆☆ | ★★★☆☆ |
| Windows-Dienst (15%) | ★★★★☆ | ★★★★★ | ★★☆☆☆ |
| Sicherheits-Framework (15%) | ★★★★★ | ★★★★★ | ★★★★☆ |
| SQL Server Integration (10%) | ★★★★☆ | ★★★★★ | ★★★☆☆ |
| Langlebigkeit/LTS (10%) | ★★★★★ | ★★★★☆ | ★★★☆☆ |
| Entwicklungsgeschwindigkeit (5%) | ★★★★☆ | ★★★★☆ | ★★★★★ |
| **Gesamt** | **4,55** | **3,85** | **2,80** |

## Konsequenzen

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
- Build-System ist Maven (Standard für Spring Boot Projekte)
- Java 17 als Minimum, späteres Upgrade auf Java 21 LTS problemlos möglich
