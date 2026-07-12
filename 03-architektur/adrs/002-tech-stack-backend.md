# ADR-002: Technologie-Stack Backend – Spring Boot (Java 21)

## Status

**Akzeptiert** – Juni 2026

## Kontext

Gemäß ADR-001 wird eine 3-Schichten-Architektur mit zentralem REST-Backend implementiert.
Für die Anwendungsschicht muss eine Technologie gewählt werden, die folgende Anforderungen erfüllt:

| Anforderung | Beschreibung |
|-------------|-------------|
| **Office-Dokumentengenerierung** | Excel-Reports (Mitgliederlisten, Spendenübersichten) und Word-Briefe (.docx Serienbriefe, Spendenquittungen) |
| **PostgreSQL-Anbindung** | Relationale Open-Source-Datenbank, keine Lizenzkosten, keine Größenlimitierungen |
| **Sicherheit** | JWT-Authentifizierung, RBAC, Passwort-Hashing, HTTPS |
| **Windows-Dienst** | Betrieb als automatisch startender Windows Service ohne manuelle Interaktion |
| **Wartbarkeit** | Automatische DB-Migrationen, zentrales Logging, Health Checks |
| **Teamkompetenz** | Das Legacy-System ist in Java geschrieben – Java-Grundkenntnisse vorhanden |
| **Langlebigkeit** | Für einen Verein muss die Technologie über 5–10 Jahre stabil und gewartet bleiben |

## Entscheidung

Wir entscheiden uns für **Spring Boot 3.4.x mit Java 21 LTS** (OpenJDK / Eclipse Temurin)
als Backend-Technologie, deployed als Fat-JAR und betrieben als Windows-Dienst via WinSW.

## Betrachtete Alternativen

### Alternative A: C# / ASP.NET Core (.NET 8)

| Aspekt | Bewertung |
|--------|-----------|
| Office-Dokumente | ✅ OpenXML SDK (Microsoft-nativ), NPOI (Apache-POI-Port) |
| PostgreSQL | ✅ Npgsql + Entity Framework Core – ausgereift |
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
| PostgreSQL | ✅ psycopg2/psycopg3 – exzellente Integration, Django ORM erstklassig |
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

### Alternative C: Spring Boot 3.x / Java 21 (gewählt) ✅

| Aspekt | Bewertung |
|--------|-----------|
| Office-Dokumente | ✅ Apache POI 5.x (Excel/Word), docx4j (komplexe .docx), XDocReport (Serienbriefe mit Freemarker/Velocity) |
| PostgreSQL | ✅ PostgreSQL JDBC Driver (org.postgresql), erstklassige Spring Data JPA Integration |
| Windows-Integration | ✅ WinSW als Windows Service, stabil seit Jahren im Einsatz |
| Sicherheit | ✅ Spring Security 6 (JWT, BCrypt, RBAC, CSRF, CORS), produktionserprobt |
| Performance | ✅ JVM-Performance für I/O-lastige Anwendungen hervorragend, Virtual Threads (Java 21) |
| Ökosystem | ✅ Maven Central, größtes Java-Ökosystem, Spring Initializr |
| Teamkompetenz | ✅ Java-Grundkenntnisse aus Legacy-System vorhanden |
| Migration | ✅ Geschäftslogik aus altem Java-Code teilweise übernehmbar |
| Typsicherheit | ✅ Statisch typisiert – Compile-Time-Fehler, sicheres Refactoring |
| Langlebigkeit | ✅ Java 21 LTS (Support bis 2031+), Spring Boot kommerzielle Support-Optionen |
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

### 2. PostgreSQL als Open-Source-Datenbank

PostgreSQL ist die **fortschrittlichste Open-Source-Datenbank** und bietet gegenüber dem
bisherigen MS SQL Server Express erhebliche Vorteile:

| Kriterium | PostgreSQL | MS SQL Server Express (alt) |
|-----------|-----------|----------------------|
| Lizenzkosten | ✅ Kostenlos, keine Einschränkungen | ⚠️ Kostenlos, aber limitiert (10 GB, 1 GB RAM) |
| Plattform | ✅ Windows, Linux, macOS | ❌ Nur Windows (vollwertig) |
| Community | ✅ Riesige Open-Source-Community | ⚠️ Microsoft-getrieben |
| Features | ✅ JSONB, Full-Text-Search, Window Functions, CTEs | ✅ Vergleichbar |
| Spring Integration | ✅ PostgreSQL JDBC Driver + Spring Data JPA | ✅ Erstklassig |
| Flyway-Support | ✅ Vollständig | ✅ Vollständig |
| Zukunftssicherheit | ✅ Kein Vendor-Lock-in | ⚠️ Microsoft-Abhängigkeit |

Spring Data JPA eliminiert SQL-Injection **strukturell** durch Prepared Statements.
Der PostgreSQL JDBC Driver ist ausgereift und wird aktiv gepflegt. Die JPA-Abstraktionsschicht
sorgt dafür, dass die Geschäftslogik datenbankunabhängig bleibt.

### 3. Java 21 LTS – Modernste Sprachfeatures

Java 21 (LTS, September 2023) bietet gegenüber Java 17 signifikante Verbesserungen:

| Feature | Nutzen für das Projekt |
|---------|----------------------|
| **Virtual Threads (JEP 444)** | Leichtgewichtige Threads für parallele DB-/IO-Operationen ohne Thread-Pool-Tuning |
| **Pattern Matching (JEP 441)** | Elegantere switch-Ausdrücke für Validierungslogik |
| **Record Patterns (JEP 440)** | Kompaktere DTOs und Value Objects |
| **Sequenced Collections (JEP 431)** | Bessere Collection-APIs für Listendarstellung |
| **LTS bis 2031+** | Längerer Support-Zeitraum als Java 17 (bis 2029) |

### 4. Vorhandene Teamkompetenz

Das Legacy-System ist in Java geschrieben. Das Team kann:
- Bestehende Geschäftslogik (Berechnungen, Validierungen) verstehen und portieren
- Java-Syntax und -Semantik ohne Einarbeitungszeit nutzen
- Von Java 1.4 auf Java 21 aufbauen (gleiche Sprache, modernisierte Features)

Ein Wechsel zu C# oder Python würde eine komplette Spracheinarbeitung erfordern –
unverhältnismäßig für ein zeitlich begrenztes Hochschulprojekt.

### 5. Convention over Configuration

Spring Boot minimiert Konfigurationsaufwand durch Auto-Configuration:
- Embedded Tomcat (kein separater Application Server)
- Auto-konfigurierte DataSource, JPA, Security
- Fat-JAR-Packaging: **eine Datei** = gesamte Anwendung
- Spring Initializr für Projektsetup in Minuten

### 6. Langfristige Stabilität

| Kriterium | Java 21 / Spring Boot |
|-----------|----------------------|
| LTS-Support | Java 21: bis mind. September 2031 (Temurin) |
| Framework-Reife | Spring Framework: seit 2003, Spring Boot: seit 2014 |
| Abwärtskompatibilität | Java ist bekannt für strenge Rückwärtskompatibilität |
| Community | >10 Mio. Java-Entwickler weltweit (TIOBE #1–3 seit 25 Jahren) |
| Nachbesetzung | Einfach qualifizierte Entwickler zu finden |

### 7. Windows-Dienst-Betrieb

WinSW (Windows Service Wrapper) ist eine bewährte Lösung, um Java-Anwendungen als
Windows-Dienste zu betreiben:
- Automatischer Start bei Systemboot
- Automatischer Neustart bei Crash
- Service-Account (kein interaktiver Login nötig)
- Triviale Installation (`frauenhaus-service.exe install`)

## Entscheidungsmatrix (gewichtete Bewertung)

| Kriterium (Gewicht) | Spring Boot/Java 21 | ASP.NET Core/C# | Django/Python |
|---------------------|:-------------------:|:----------------:|:-------------:|
| Office-Dokumente (25%) | ★★★★★ | ★★★★☆ | ★★☆☆☆ |
| Teamkompetenz (20%) | ★★★★★ | ★★☆☆☆ | ★★★☆☆ |
| Windows-Dienst (15%) | ★★★★☆ | ★★★★★ | ★★☆☆☆ |
| Sicherheits-Framework (15%) | ★★★★★ | ★★★★★ | ★★★★☆ |
| PostgreSQL Integration (10%) | ★★★★★ | ★★★★★ | ★★★★★ |
| Langlebigkeit/LTS (10%) | ★★★★★ | ★★★★☆ | ★★★☆☆ |
| Entwicklungsgeschwindigkeit (5%) | ★★★★☆ | ★★★★☆ | ★★★★★ |
| **Gesamt** | **4,65** | **3,85** | **2,90** |

## Konsequenzen

### Positiv
- Direkter Wissenstransfer vom Legacy-Java-Code möglich
- Bestmögliche Unterstützung für Office-Dokumentengenerierung (POI, XDocReport)
- Spring Security löst alle identifizierten Sicherheitsprobleme des IST-Systems
- Fat-JAR-Deployment vereinfacht Updates auf ein Minimum (JAR austauschen, Dienst neustarten)
- Flyway-Integration automatisiert Datenbankmigrationen
- Actuator-Endpoints ermöglichen Monitoring ohne zusätzliche Tools
- Virtual Threads (Java 21) vereinfachen parallele Verarbeitung ohne komplexes Thread-Management
- PostgreSQL eliminiert Lizenzkosten und Größenlimitierungen des bisherigen SQL Server Express

### Negativ
- JVM-Startup ist langsamer als native Anwendungen (~3–5 Sekunden) – für einen
  Windows-Dienst irrelevant, da nur einmal gestartet
- Höherer Speicherverbrauch als Python/Go (~200–400 MB) – akzeptabel auf dediziertem Server
- Spring Boot hat steile Lernkurve bei fortgeschrittenen Features (Security-Konfiguration) –
  mitigiert durch exzellente Dokumentation und Community
- Datenmigration von MS SQL Server zu PostgreSQL erfordert einmaligen Migrationsaufwand

### Neutral
- Frontend-Technologie (Angular) muss separat entschieden werden (unabhängig vom Backend-Stack)
- Build-System ist Maven (Standard für Spring Boot Projekte)
- Java 21 als Basis, späteres Upgrade auf Java 25 LTS problemlos möglich
- PostgreSQL läuft als separater Windows-Dienst auf demselben Server
