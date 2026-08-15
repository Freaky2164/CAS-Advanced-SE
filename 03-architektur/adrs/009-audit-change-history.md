# ADR-009: Änderungshistorie / Audit – Hibernate Envers

## Status

**Akzeptiert** – Juli 2026

## Kontext

Das IST-System bildete eine Änderungshistorie über anwendungsseitige Konstrukte ab
(`CDisplayFieldHistoryBean`, `CHistoryDialog`) – manuell, uneinheitlich und schwer wartbar. Für die
neue Architektur ergeben sich mehrere Treiber für eine **systematische Auditierung**:

- **DSGVO-Nachvollziehbarkeit (NFR-1)**: Bei der Verarbeitung personenbezogener und teils besonders
  sensibler Daten (Art. 9 DSGVO) muss nachvollziehbar sein, **wer wann welchen Datensatz geändert**
  hat. Dies ist als Qualitätsszenario **QS-8** (Auditierbarkeit) verankert.
- **Fachliche Nachvollziehbarkeit**: Spenden, Bußgelder und Zahlungseingänge sind
  finanzrelevant – Änderungen müssen historisiert werden.
- **Wartbarkeit (NFR-3)**: Die Historisierung soll als **Querschnitt** ohne Verschmutzung der
  Fachlogik erfolgen (Abschnitt 4.2 der finalen Architektur).

## Entscheidung

Wir setzen **Hibernate Envers** ein. Auditpflichtige JPA-Entities werden mit `@Audited` annotiert;
Envers führt je Entität eine **`*_aud`-Tabelle** sowie eine zentrale Revisionstabelle (`revinfo`).
Ein **`RevisionListener`** reichert jede Revision mit Zeitpunkt und **Benutzer** (aus dem Spring
`SecurityContext`, ADR-004) an. Die Audit-Tabellen unterliegen der Schemaverwaltung durch Flyway und
werden vom Backup (ADR-005) mit abgedeckt.

## Betrachtete Alternativen

### Alternative A: Datenbank-Trigger + History-Tabellen
Historisierung in der Datenbank per Trigger. Wirkt auch bei Zugriffen außerhalb der Anwendung, ist
aber **datenbankspezifisch**, liegt außerhalb der JPA-/Service-Schicht und ist damit eher Aufgabe der
Datenbank-Gruppe. Der auslösende **Benutzer** aus dem Anwendungskontext ist auf DB-Ebene nur
umständlich verfügbar.

### Alternative B: System-Versionierung / Temporal Tables
PostgreSQL bietet keine native SQL:2011-System-Versionierung; sie erfordert Extensions/Eigenbau und
zusätzlichen Betrieb – unverhältnismäßig.

### Alternative C: Manuelles anwendungsseitiges Audit
Explizites Schreiben von History-Einträgen in den Services. Vollständig flexibel, aber
**fehleranfällig** (leicht vergessen), viel Boilerplate und Vermischung mit der Fachlogik – genau die
Schwäche des Alt-Systems.

### Alternative D: Change Data Capture (Debezium/Kafka)
Ereignisstrom-basierte Erfassung. Mächtig, aber mit Broker-Infrastruktur **massiv überdimensioniert**
für einen kleinen On-Premises-Verein (Widerspruch zu ADR-006).

### Alternative E: Hibernate Envers (gewählt) ✅
Automatische, deklarative Historisierung, nahtlos in JPA/Hibernate (ADR-008) integriert, erfasst den
Anwendungsbenutzer über den `RevisionListener`, keine zusätzliche Infrastruktur.

## Begründung

- **Automatisch statt manuell**: `@Audited` genügt – kein Historisierungscode in den Services,
  keine „vergessenen" Einträge (behebt die Alt-System-Schwäche).
- **Querschnittlich sauber**: Auditierung bleibt getrennt von der Fachlogik (NFR-3).
- **Benutzerbezug**: Der `RevisionListener` verknüpft jede Änderung mit dem authentifizierten
  Benutzer (ADR-004) – Grundlage für QS-8 und DSGVO-Nachweise.
- **Kein Zusatzbetrieb**: Nutzt die vorhandene Persistenzschicht und das bestehende Backup.

## Konsequenzen

### Positiv
- Lückenlose, automatische Änderungshistorie je auditierter Entität
- Nachvollziehbarkeit „wer/wann" für DSGVO- und Finanzrelevanz (QS-8)
- Keine zusätzliche Infrastruktur

### Negativ
- `*_aud`-Tabellen vergrößern die Datenbank und damit das Backup-Volumen
- Schema-Migrationen (Flyway) müssen die Audit-Tabellen mitführen
- **DSGVO-Spannung**: Das Recht auf Löschung (Art. 17) muss auch die Audit-Historie berücksichtigen –
  die konkrete Umsetzung liegt im **Löschkonzept der Datenbank-Gruppe** (Abschnitt 10.7 der finalen
  Architektur), mit dem die Audit-Aufbewahrung abzustimmen ist

### Neutral
- Nur fachlich relevante Entitäten werden auditiert; reine Stammdaten-/Hilfstabellen können bewusst
  ausgenommen werden, um das Volumen zu begrenzen
