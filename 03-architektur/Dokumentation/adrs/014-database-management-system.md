# ADR-014: Datenbankmanagementsystem – PostgreSQL statt SQL Server Express

## Status

**Akzeptiert und in `Code.zip` implementiert** – August 2026

Ersetzt das im Altsystem eingesetzte Microsoft SQL Server Express als Datenbankmanagementsystem.
Steht in direktem Zusammenhang mit der Windows-Rahmenbedingung [ADR-013](013-windows-operation.md)
und der Backup-Strategie [ADR-005](005-backup-strategy.md).

## Kontext

Das Altsystem greift über direktes JDBC auf **Microsoft SQL Server Express** zu. Für die neue
Architektur ist zu entscheiden, welches DBMS den 3-Schichten-Monolithen mit Spring Data JPA,
Row-Level Security, Audit-Historie und der Ablage von Dokumenten als `bytea` tragen soll. Die
Aufgabenstellung fordert zugleich, dass die gesamte Software inklusive Datenbank unter Microsoft
Windows läuft (siehe ADR-013). Die DBMS-Wahl steht damit in einem bewussten Spannungsfeld zwischen
funktionalen/lizenzrechtlichen Anforderungen und der Windows-Rahmenbedingung.

SQL Server Express bringt für den Anwendungsfall mehrere Einschränkungen mit:

- **Größenlimit**: Express begrenzt die Datenbankgröße (historisch 10 GB je Datenbank), was mit
  der geplanten Ablage von Dokumenten als `bytea` mittelfristig kollidieren kann.
- **Ressourcenlimits**: begrenzte CPU-/RAM-Nutzung der Express-Edition.
- **Lizenz-/Ökosystem-Bindung**: Bindung an das Microsoft-Ökosystem, während die Zielarchitektur
  einen containerisierten, plattformunabhängigen Betrieb anstrebt (ADR-011).

## Entscheidung

Als Datenbankmanagementsystem wird **PostgreSQL 18** eingesetzt.

PostgreSQL wird im containerisierten Linux-Betrieb unter Windows ausgeführt (ADR-013). Die Wahl
stützt sich auf offene Lizenzierung, das Fehlen der Express-Größen- und Ressourcenlimits, robuste
Transaktions- und Constraint-Unterstützung, native Row-Level Security sowie die enge Integration
mit Spring Data JPA und Hibernate Envers.

Nachweis ist die Docker-Compose-Konfiguration mit PostgreSQL-Service sowie die
Flyway-Migrationen innerhalb von `Code.zip`.

## Betrachtete Alternativen

### Alternative A: Verbleib bei Microsoft SQL Server Express

| Aspekt | Bewertung |
|--------|-----------|
| Windows-Konformität | ✅ native Windows-Unterstützung |
| Migrationsaufwand | ✅ gering, da bereits im Einsatz |
| Größenlimit | ❌ begrenzte Datenbankgröße, kritisch bei `bytea`-Dokumenten |
| Ressourcenlimits | ❌ eingeschränkte CPU-/RAM-Nutzung |
| Lizenz-/Editionswechsel | ⚠️ Wegfall der Limits nur über kostenpflichtige Editionen |
| Row-Level Security | ✅ vorhanden, aber Ökosystem-gebunden |

**Ablehnung**: Die Größen- und Ressourcenlimits der Express-Edition sind mit der geplanten
Dokumentenablage und dem angestrebten plattformunabhängigen Betrieb nicht vereinbar; ein Wechsel
auf eine limitfreie Edition wäre lizenzpflichtig.

### Alternative B: MySQL/MariaDB

| Aspekt | Bewertung |
|--------|-----------|
| Lizenz | ✅ offen |
| Row-Level Security | ❌ keine native RLS wie in PostgreSQL |
| Feature-Tiefe (Constraints, JSON, Erweiterungen) | ⚠️ geringer als PostgreSQL |
| Spring-Data-JPA-Integration | ✅ gut |

**Ablehnung**: Die für das Sicherheitskonzept zentrale Row-Level Security ist in PostgreSQL nativ
verfügbar, in MySQL/MariaDB hingegen nicht in gleicher Form. Damit entfällt ein tragender
Baustein der Defense-in-Depth-Strategie (siehe ADR-012).

### Alternative C: PostgreSQL (gewählt) ✅

| Aspekt | Bewertung |
|--------|-----------|
| Lizenz | ✅ offen, kostenfrei |
| Größen-/Ressourcenlimit | ✅ keine editionsbedingten Limits |
| Row-Level Security | ✅ nativ, tragend für das Sicherheitskonzept |
| Transaktionen/Constraints | ✅ ausgereift |
| Spring Data JPA / Envers | ✅ enge Integration |
| Windows-Betrieb | ⚠️ nicht nativ als Fokus; über ADR-013 (Container/WSL 2) gelöst |
| Backupwerkzeug pgBackRest | ✅ verfügbar (Linux), siehe ADR-005 |

## Begründung

- **Row-Level Security**: PostgreSQL bietet native RLS, die im Sicherheitskonzept als zweite
  Durchsetzungsebene neben der Anwendungsautorisierung dient (ADR-012).
- **Keine Editionslimits**: Größe und Ressourcennutzung sind nicht künstlich beschränkt, was die
  Ablage von Dokumenten als `bytea` und ein einheitliches Backup über die Datenbank ermöglicht
  (vgl. ADR-005).
- **Ökosystem und Integration**: enge Anbindung an Spring Data JPA, Hibernate Envers und Flyway;
  breite Werkzeugunterstützung inkl. pgBackRest für die Zielbackupstrategie.
- **Offene Lizenzierung**: passend zum Budgetrahmen eines spendenfinanzierten Vereins.

Die Wahl eines DBMS garantiert für sich genommen weder Konsistenz noch Datenschutz. Entscheidend
bleiben Schema, Constraints, Rollen, Migrationen, Backup und Betrieb; PostgreSQL ist die Grundlage,
auf der diese Bausteine umgesetzt werden.

## Spannungsfeld zur Windows-Rahmenbedingung

PostgreSQL adressiert die geforderte Windows-Ausführung nicht aus sich heraus. Diese Spannung wird
bewusst nicht ignoriert, sondern in ADR-013 aufgelöst: Der PostgreSQL-Container läuft unter Docker
Desktop bzw. Docker Engine mit WSL 2 auf einem Windows-Host und ist damit ein unter Windows
verwalteter Dienst. Verlangt eine spätere Ausschreibung eine Container-freie Installation, ist der
Betrieb von PostgreSQL als nativer Windows-Dienst die dokumentierte Rückfalloption (ADR-013), was
eine Umstellung der Backupstrategie auf ein Windows-fähiges Verfahren nach sich zöge.

## Konsequenzen

### Positiv

- Keine editionsbedingten Größen- und Ressourcenlimits.
- Native Row-Level Security stützt das Defense-in-Depth-Sicherheitskonzept.
- Offene Lizenz ohne Kosten und enge Spring-/JPA-Integration.

### Negativ

- PostgreSQL ist nicht primär auf nativen Windows-Betrieb ausgerichtet; die Windows-Konformität
  wird erst durch ADR-013 (Container/WSL 2) hergestellt.
- Der Wechsel weg von SQL Server erfordert Schema-, Query- und Betriebsanpassungen gegenüber dem
  Altsystem.

### Neutral

- Die Backupstrategie (ADR-005) ist auf pgBackRest im Linux-Kontext ausgelegt; im nativen
  Windows-Fallback müsste sie auf `pg_basebackup`/`pg_dump` umgestellt werden.
- Die konkrete PostgreSQL-Minor-Version kann im Rahmen der Wartung angehoben werden, solange
  Migrationen und Restore-Tests dies begleiten.
