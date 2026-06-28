# ADR-001: Architekturstil – 3-Schichten-Architektur statt Fat-Client

## Status

**Akzeptiert** – Juni 2026

## Kontext

Das bestehende System ist eine Java-Swing-Desktop-Anwendung (ca. 2005–2013) mit klassischer
2-Schichten-Architektur: Ein Fat Client pro Arbeitsplatz kommuniziert direkt per JDBC mit
einer MS SQL Server Express Datenbank. Dieses Modell verursacht erhebliche Probleme:

- **Sicherheit**: Datenbank-Zugangsdaten liegen im Klartext auf jedem Client-PC (.ini-Dateien),
  SQL-Queries werden per String-Konkatenation gebaut (SQL-Injection-Anfälligkeit),
  kein Rollenkonzept auf Anwendungsebene
- **Wartung**: Jede Änderung erfordert Neuinstallation auf allen PCs, kein zentrales Logging,
  kein automatisiertes Update-Verfahren
- **Betrieb**: Keine Zwischenschicht für Geschäftslogik, Validierung und Zugriffskontrolle

Es wird eine neue Architektur benötigt, die diese Schwachstellen adressiert und gleichzeitig
im Rahmen eines kleinen Teams (Verein mit wenigen Arbeitsplätzen, kein IT-Personal) betreibbar bleibt.

## Entscheidung

Wir entscheiden uns für eine **3-Schichten-Architektur** (Präsentation → Anwendungslogik → Daten)
mit folgender Aufteilung:

1. **Präsentationsschicht**: Web-Frontend (Single-Page-Application) im Browser
2. **Anwendungsschicht**: Zentrales REST-Backend als Windows-Dienst auf dem Server
3. **Datenschicht**: PostgreSQL auf demselben Server

```
Browser (SPA)  ──HTTPS/REST──►  Backend (Windows-Dienst)  ──JDBC/SSL──►  PostgreSQL
```

## Betrachtete Alternativen

### Alternative A: Modernisierter Fat Client (2-Schichten beibehalten)

Beibehaltung der 2-Schichten-Architektur mit modernisiertem Desktop-Client (z.B. JavaFX statt Swing).

| Aspekt | Bewertung |
|--------|-----------|
| Client-Deployment | ❌ Weiterhin Installation auf jedem PC nötig |
| Sicherheit | ❌ DB-Credentials weiterhin auf Clients, kein zentraler Sicherheitsfilter |
| Updates | ❌ Jedes Update muss auf allen PCs eingespielt werden |
| Offline-Fähigkeit | ✅ Lokale Anwendung funktioniert ohne Netzwerk |
| Komplexität | ✅ Einfachere Gesamtarchitektur (weniger Komponenten) |

**Ablehnung**: Das Kernproblem – verteilte Installation, fehlende Sicherheitsschicht,
wartungsintensives Deployment – bleibt bestehen.

### Alternative B: Microservices-Architektur

Aufteilung der Geschäftslogik in mehrere unabhängige Services (Mitglieder-Service,
Spenden-Service, Bußgeld-Service, Auth-Service).

| Aspekt | Bewertung |
|--------|-----------|
| Skalierbarkeit | ✅ Einzelne Services unabhängig skalierbar |
| Technologiefreiheit | ✅ Jeder Service kann andere Technologie nutzen |
| Betriebskomplexität | ❌ Service Discovery, API Gateway, verteiltes Logging nötig |
| Infrastruktur | ❌ Erfordert Container-Orchestrierung (Docker/Kubernetes) |
| Teamgröße | ❌ Für ein kleines Team (3–5 Nutzer, kein IT-Personal) massiv überdimensioniert |
| Datenkonsistenz | ❌ Verteilte Transaktionen zwischen Services komplex |

**Ablehnung**: Unverhältnismäßiger Betriebsaufwand für eine Vereinsanwendung mit
wenigen Nutzern. Das System hat keine Skalierungsanforderungen, die Microservices rechtfertigen.

### Alternative C: 3-Schichten mit Web-Frontend (gewählt) ✅

| Aspekt | Bewertung |
|--------|-----------|
| Client-Deployment | ✅ Kein Deployment – nur Browser nötig |
| Sicherheit | ✅ Zentrale Authentifizierung/Autorisierung im Backend, kein DB-Zugriff vom Client |
| Updates | ✅ JAR austauschen + Dienst neustarten – alle Clients sofort aktuell |
| Wartung | ✅ Ein Server, eine Konfiguration, zentrales Logging |
| Betriebskomplexität | ✅ Ein Windows-Dienst + SQL Server – beherrschbar ohne IT-Abteilung |
| Offline-Fähigkeit | ⚠️ Erfordert Netzwerkverbindung zum Server (akzeptabel im LAN) |

## Begründung

1. **Eliminierung des Hauptproblems**: Die zentrale Schwachstelle des IST-Systems – verteilte
   Fat Clients mit direktem DB-Zugriff – wird vollständig eliminiert. Kein Client hat mehr
   Datenbank-Credentials oder kann SQL-Queries direkt absetzen.

2. **Defence in Depth**: Die Anwendungsschicht bildet eine verbindliche Sicherheitsbarriere
   zwischen Nutzer und Datenbank (Authentifizierung, Autorisierung, Input-Validierung,
   Prepared Statements).

3. **Operationale Einfachheit**: Ein einziger Server mit zwei Diensten (Backend + SQL Server)
   ist für einen Verein ohne IT-Personal betreibbar. Updates erfolgen durch JAR-Austausch
   in unter 10 Minuten.

4. **Zukunftsfähigkeit**: Die REST-API ermöglicht später zusätzliche Clients (Mobile App,
   Automatisierungsskripte) ohne Backend-Änderung.

5. **Verhältnismäßigkeit**: Die 3-Schichten-Architektur bietet den optimalen Kompromiss
   zwischen Sicherheitsgewinn und Betriebskomplexität für den gegebenen Kontext
   (kleiner Verein, LAN-Betrieb, wenige Nutzer).

## Konsequenzen

### Positiv
- Kein Client-Deployment mehr – drastisch reduzierter Wartungsaufwand
- Zentrale Sicherheitsschicht verhindert SQL-Injection und unbefugten Zugriff strukturell
- Einheitliche Konfiguration und Logging auf einem Server
- Automatisierte Datenbankmigrationen (Flyway) bei jedem Update

### Negativ
- Netzwerkabhängigkeit: Ohne LAN-Verbindung kein Zugriff (akzeptabel für Büro-Anwendung)
- Höhere initiale Entwicklungskomplexität (Backend + Frontend statt nur Desktop-App)
- Server wird zum Single Point of Failure (mitigiert durch automatischen Dienst-Neustart und Backup)

### Neutral
- Erfordert Entscheidung über konkreten Technologie-Stack (siehe ADR-002)
- Frontend- und Backend-Entwicklung können nach API-Definition parallelisiert werden
