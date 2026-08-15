# ADR-002: Technologie-Stack Backend – Spring Boot (Java 25)

## Status

**Akzeptiert** – Juni 2026

## Kontext

Gemäß ADR-001 wird eine 3-Schichten-Architektur mit zentralem Backend implementiert (die
Präsentationsschicht bindet die Anwendungslogik direkt an, ohne separate REST-Schicht – Vaadin, ADR-003).
Für die Anwendungsschicht muss eine Technologie gewählt werden, die folgende Anforderungen erfüllt:

| Anforderung | Beschreibung |
|-------------|-------------|
| **Office-Dokumentengenerierung** | Excel-Reports (Mitgliederlisten, Spendenübersichten) und Word-Briefe (.docx Serienbriefe, Spendenquittungen) |
| **PostgreSQL-Anbindung** | Relationale Open-Source-Datenbank, keine Lizenzkosten, keine Größenlimitierungen |
| **Sicherheit** | Authentifizierung, RBAC, Passwort-Hashing, HTTPS (Verfahren wird in ADR-004 festgelegt) |
| **Windows-Dienst** | Betrieb als automatisch startender Windows Service ohne manuelle Interaktion |
| **Wartbarkeit** | Automatische DB-Migrationen, zentrales Logging, Health Checks |
| **Legacy-Wissenstransfer** | Das Legacy-System ist in Java geschrieben; Geschäftslogik und Domänenmodell sollen les- und portierbar bleiben |
| **Langlebigkeit** | Für einen Verein muss die Technologie über 5–10 Jahre stabil und gewartet bleiben |

## Entscheidung

Wir entscheiden uns für **Spring Boot 4.x mit Java 25 LTS** (OpenJDK / Eclipse Temurin)
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
| Legacy-Wissenstransfer / Migration | ❌ Bestehende Java-Geschäftslogik nur durch vollständiges Neuschreiben nutzbar |
| Lizenzkosten | ⚠️ Visual Studio Professional kostenpflichtig (Community Edition limitiert) |
| Langlebigkeit | ✅ .NET LTS-Releases (3 Jahre Support) |

**Bewertung**: Technisch gleichwertig oder in Teilbereichen (Windows-Integration) sogar leicht
überlegen. Der entscheidende Nachteil ist fachlicher Natur: Der bestehende Java-Code
(Geschäftslogik, Berechnungen) kann nicht portiert, sondern müsste vollständig neu geschrieben
werden. Für ein Hochschulprojekt mit begrenzter Zeit ist dieser Bruch mit dem Bestandscode
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
| Legacy-Wissenstransfer / Migration | ❌ Bestehende Java-Logik nicht wiederverwendbar – Neuimplementierung nötig |
| Office-Qualität | ❌ python-docx unterstützt keine .dot/.dotx Templates nativ, keine Mail Merge |
| Typsicherheit | ❌ Dynamisch typisiert – Fehler erst zur Laufzeit, schwieriger Refactoring |
| Langlebigkeit | ⚠️ Django stabil, aber Python-Versioning (2→3 Trauma) und schnellere Breaking Changes |

**Bewertung**: Python eignet sich hervorragend für Scripting und Data Science, aber die
Office-Dokumentengenerierung ist deutlich schwächer als in Java/C#. `python-docx` unterstützt
keine Template-basierte Serienbriefgenerierung und keine komplexe Formatierung. Zudem fehlt
die robuste Windows-Dienst-Integration. Für eine langlebige Vereinsanwendung ist die
dynamische Typisierung ein Wartbarkeitsrisiko.

### Alternative C: Spring Boot 4.x / Java 25 (gewählt) ✅

| Aspekt | Bewertung |
|--------|-----------|
| Office-Dokumente | ✅ Apache POI 5.x (Excel/Word), docx4j (komplexe .docx), XDocReport (Serienbriefe mit Freemarker/Velocity) |
| PostgreSQL | ✅ PostgreSQL JDBC Driver (org.postgresql), erstklassige Spring Data JPA Integration |
| Windows-Integration | ✅ WinSW als Windows Service, stabil seit Jahren im Einsatz |
| Sicherheit | ✅ Spring Security 7 (Session-Auth/BCrypt/RBAC, CSRF, CORS – Verfahren gemäß ADR-004), produktionserprobt |
| Performance | ✅ JVM-Performance für I/O-lastige Anwendungen hervorragend, Virtual Threads (Java 21+) |
| Ökosystem | ✅ Maven Central, größtes Java-Ökosystem, Spring Initializr |
| Legacy-Wissenstransfer / Migration | ✅ Geschäftslogik & Domänenmodell aus altem Java-Code direkt les- und teilweise übernehmbar |
| Typsicherheit | ✅ Statisch typisiert – Compile-Time-Fehler, sicheres Refactoring |
| Langlebigkeit | ✅ Java 25 LTS; Supportzeitraum abhängig von Distribution und Lizenz |
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

Spring Data JPA verwendet bei korrekter Parameterbindung Prepared Statements und reduziert damit
klassische SQL-Injection-Risiken erheblich. Native oder dynamisch zusammengesetzte Abfragen
müssen weiterhin gesondert geprüft werden.
Der PostgreSQL JDBC Driver ist ausgereift und wird aktiv gepflegt. Die JPA-Abstraktionsschicht
sorgt dafür, dass die Geschäftslogik datenbankunabhängig bleibt.

### 3. Java 25 LTS – Moderne Sprachfeatures

Java 25 (LTS, September 2025) bündelt die seit Java 17/21 finalisierten Neuerungen und ergänzt weitere Verbesserungen:

| Feature | Nutzen für das Projekt |
|---------|----------------------|
| **Virtual Threads (JEP 444)** | Leichtgewichtige Threads für parallele DB-/IO-Operationen ohne Thread-Pool-Tuning |
| **Pattern Matching (JEP 441)** | Elegantere switch-Ausdrücke für Validierungslogik |
| **Record Patterns (JEP 440)** | Kompaktere DTOs und Value Objects |
| **Sequenced Collections (JEP 431)** | Bessere Collection-APIs für Listendarstellung |
| **LTS-Release** | Oracle plant Extended Support bis mindestens 2033; andere Distributionen können abweichende Zeiträume besitzen |

### 4. Legacy-Wissenstransfer aus dem Bestandscode

Ausschlaggebend ist **nicht** eine überlegene Java-Erfahrung des Teams – die Mitglieder sind
in mehreren Sprachen (u. a. Java, C#, Python) vergleichbar versiert. Entscheidend ist vielmehr
die objektive Ausgangslage: **Die zu migrierende Geschäftslogik und das Domänenmodell liegen
bereits in Java vor.** Java als Zielsprache erlaubt daher:
- direktes Lesen der bestehenden Berechnungs- und Validierungsregeln als fachliche Referenz
- schrittweises Portieren statt vollständiger Neuimplementierung
- Erhalt der fachlichen Semantik ohne Übersetzungsfehler zwischen Sprachen

Ein Wechsel zu C# oder Python böte keinen dieser Vorteile: Die bestehende Java-Logik müsste
vollständig neu geschrieben werden – ein vermeidbares fachliches Risiko in einem zeitlich
begrenzten Projekt. Dieser Vorteil ist damit an den **Bestandscode** geknüpft, nicht an eine
unterstellte Sprachpräferenz oder ein höheres Java-Skill-Level des Teams.

### 5. Convention over Configuration

Spring Boot minimiert Konfigurationsaufwand durch Auto-Configuration:
- Embedded Tomcat (kein separater Application Server)
- Auto-konfigurierte DataSource, JPA, Security
- Fat-JAR-Packaging: **eine Datei** = gesamte Anwendung
- Spring Initializr für Projektsetup in Minuten

### 6. Langfristige Stabilität

| Kriterium | Java 25 / Spring Boot |
|-----------|----------------------|
| LTS-Support | Java 25: Oracle NFTC bis 2028, kommerzieller Extended Support bis mindestens 2033; distributionsabhängig |
| Framework-Reife | Spring Framework: seit 2003, Spring Boot: seit 2014 |
| Abwärtskompatibilität | Java ist bekannt für strenge Rückwärtskompatibilität |
| Community | großes, langjährig etabliertes Java- und Spring-Ökosystem |
| Nachbesetzung | Einfach qualifizierte Entwickler zu finden |

### 7. Windows-Dienst-Betrieb

WinSW (Windows Service Wrapper) ist eine bewährte Lösung, um Java-Anwendungen als
Windows-Dienste zu betreiben:
- Automatischer Start bei Systemboot
- Automatischer Neustart bei Crash
- Service-Account (kein interaktiver Login nötig)
- Triviale Installation (`frauenhaus-service.exe install`)

## Entscheidungsmatrix (gewichtete Bewertung)

### Herleitung der Gewichte

Die Gewichte sind **nicht frei gewählt**, sondern aus den vier tragenden Qualitätssäulen
(01-requirements.md) und den funktionalen Anforderungen abgeleitet. Damit die Bewertung
nachvollziehbar und nicht ergebnisorientiert ist, wird jedes Gewicht begründet:

| Kriterium | Gewicht | Ableitung / Begründung |
|-----------|:-------:|------------------------|
| Office-Dokumente | 25 % | FR-8, FR-9, FR-10 (Auswertungen, Spendenbescheinigungen, Serienbriefe) sind zentrale, differenzierende Fachfunktionen und historisch die aufwändigsten Bausteine des Alt-Systems; hier unterscheiden sich die Ökosysteme am stärksten |
| Legacy-Wissenstransfer / Migration | 20 % | Die zu migrierende Geschäftslogik liegt bereits in Java vor; eine Java-Zielsprache macht sie direkt les- und portierbar statt neu implementierbar (NFR-2/NFR-3, Migrationsrisiko). Bewertet wird die objektive **Bestandscode-Nähe**, ausdrücklich **nicht** eine unterstellte Java-Überlegenheit des Teams |
| Windows-Dienst | 15 % | ADR-006 (On-Premises, Windows-Server, unbeaufsichtigter Betrieb) macht native Dienst-Integration betriebskritisch |
| Sicherheits-Framework | 15 % | NFR-1 (DSGVO) und die im IST-System fehlende Sicherheitsschicht (ADR-001) sind harte Anforderungen |
| PostgreSQL-Integration | 10 % | Wichtig, aber bei allen Kandidaten ausgereift → geringe Differenzierung, daher moderat gewichtet |
| Langlebigkeit / LTS | 10 % | Verein ohne IT-Personal benötigt 5–10 Jahre Stabilität, differenziert die Kandidaten aber nur mäßig |
| Entwicklungsgeschwindigkeit | 5 % | Relevant, aber der Einmalaufwand ist gegenüber langfristiger Wartbarkeit nachrangig |

Summe = 100 %. Die beiden höchsten Gewichte (Office-Dokumente, Legacy-Wissenstransfer) folgen
direkt aus der fachlich risikoreichsten Anforderung bzw. der objektiven Ausgangslage
(Bestandscode in Java) – nicht aus einer Vorfestlegung auf Java oder einer unterstellten
Sprachpräferenz des Teams.

| Kriterium (Gewicht) | Spring Boot/Java 25 | ASP.NET Core/C# | Django/Python |
|---------------------|:-------------------:|:----------------:|:-------------:|
| Office-Dokumente (25%) | ★★★★★ | ★★★★☆ | ★★☆☆☆ |
| Legacy-Wissenstransfer (20%) | ★★★★★ | ★★☆☆☆ | ★★☆☆☆ |
| Windows-Dienst (15%) | ★★★★☆ | ★★★★★ | ★★☆☆☆ |
| Sicherheits-Framework (15%) | ★★★★★ | ★★★★★ | ★★★★☆ |
| PostgreSQL Integration (10%) | ★★★★★ | ★★★★★ | ★★★★★ |
| Langlebigkeit/LTS (10%) | ★★★★★ | ★★★★☆ | ★★★☆☆ |
| Entwicklungsgeschwindigkeit (5%) | ★★★★☆ | ★★★★☆ | ★★★★★ |
| **Gesamt** | **4,80** | **4,00** | **2,85** |

**Sensitivitätsbetrachtung**: Selbst wenn man das ergebnistreibende Kriterium „Legacy-Wissenstransfer"
auf 0 % setzt und gleichmäßig auf die übrigen Kriterien umverteilt, bleibt Spring Boot/Java 25
mit 4,73 vor ASP.NET Core (4,50) – die Entscheidung ist gegenüber genau dem am stärksten
„pro Java" wirkenden Gewicht robust.

## Konsequenzen

### Positiv
- Direkter Wissenstransfer vom Legacy-Java-Code möglich
- Bestmögliche Unterstützung für Office-Dokumentengenerierung (POI, XDocReport)
- Spring Security löst alle identifizierten Sicherheitsprobleme des IST-Systems
- Fat-JAR-Deployment vereinfacht Updates auf ein Minimum (JAR austauschen, Dienst neustarten)
- Flyway-Integration automatisiert Datenbankmigrationen
- Actuator-Endpoints ermöglichen Monitoring ohne zusätzliche Tools
- Virtual Threads (Java 21+) vereinfachen parallele Verarbeitung ohne komplexes Thread-Management
- PostgreSQL eliminiert Lizenzkosten und Größenlimitierungen des bisherigen SQL Server Express

### Negativ
- JVM-Startup ist langsamer als native Anwendungen (~3–5 Sekunden) – für einen
  Windows-Dienst irrelevant, da nur einmal gestartet
- Höherer Speicherverbrauch als Python/Go (~200–400 MB) – akzeptabel auf dediziertem Server
- Spring Boot hat steile Lernkurve bei fortgeschrittenen Features (Security-Konfiguration) –
  mitigiert durch exzellente Dokumentation und Community
- Datenmigration von MS SQL Server zu PostgreSQL erfordert einmaligen Migrationsaufwand

### Neutral
- Frontend-Technologie muss separat entschieden werden (unabhängig vom Backend-Stack); die
  Wahl fällt in ADR-003 auf Vaadin (Full-Stack Java)
- Build-System ist Maven (Standard für Spring Boot Projekte)
- Java 25 als aktuelle LTS-Basis; künftige Upgrades bleiben planungs- und testpflichtig
- PostgreSQL läuft als separater Windows-Dienst auf demselben Server
