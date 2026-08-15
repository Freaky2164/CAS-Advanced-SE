# ADR-008: Persistenz-/ORM-Strategie & Dokumentenablage – Spring Data JPA (Hibernate) mit Dokumenten als `bytea`

## Status

**Akzeptiert** – Juli 2026

## Kontext

Gemäß ADR-002 (Backend Spring Boot) und ADR-006 (On-Premises, PostgreSQL) muss die
Anwendungsschicht auf die relationale Datenbank zugreifen. Zwei eng verwandte, architektonisch
signifikante Entscheidungen sind zu treffen:

1. **Zugriffs-/ORM-Strategie**: Wie greift das Backend auf PostgreSQL zu? Das IST-System baute
   SQL per String-Konkatenation (SQL-Injection-Risiko, ADR-001). Gefordert sind strukturelle
   Injection-Sicherheit, datenbankunabhängige Geschäftslogik und geringer Boilerplate.
2. **Dokumentenablage**: Die fachlich zentralen Dokumente (Spendenbescheinigungen FR-9,
   Serienbriefe FR-10) müssen gespeichert und **gesichert** werden (ADR-005). Wo liegen sie?

## Entscheidung

1. **Spring Data JPA mit Hibernate** als ORM/Persistenzabstraktion. Datenzugriff ausschließlich über
   `*Repository extends JpaRepository` (Repository-Muster, Abschnitt 4.2 der finalen Architektur),
   Abfragen über abgeleitete Methoden, JPQL oder Criteria – alle als **Prepared Statements**.
2. **Dokumente werden als `bytea`** in der Entity `Dokument` **direkt in PostgreSQL** gespeichert
   (nicht im Dateisystem, nicht in Objektspeicher).

## Betrachtete Alternativen

### ORM-/Zugriffsschicht

#### Alternative A: Plain JDBC / `JdbcTemplate`
Volle SQL-Kontrolle, aber viel Boilerplate für Mapping, manuelle Transaktions-/Beziehungs­pflege und
kein einheitliches Repository-Modell. Prepared Statements möglich, aber Injection-Sicherheit liegt
in der Disziplin des Entwicklers.

#### Alternative B: jOOQ
Typsichere SQL-DSL, sehr gut für komplexe Abfragen. Jedoch SQL-zentriert (weniger Domänen-/Repository-
Abstraktion), zusätzlicher Codegenerierungs-Schritt; der Mehrwert liegt v. a. bei abfrageschweren
Systemen – hier dominieren einfache CRUD-Fälle.

#### Alternative C: MyBatis
Explizites SQL-Mapping, guter Mittelweg, aber erneut mehr manuelles Mapping als JPA und keine
automatische Schema-/Beziehungsableitung.

#### Alternative D: Spring Data JPA / Hibernate (gewählt) ✅
Deklarative Repositories, automatisches Mapping des Domänenmodells (Abschnitt 4.4), standardmäßige
Parameterbindung, nahtlose Integration mit Spring-Transaktionen und
Hibernate Envers (ADR-009). Datenbankunabhängigkeit über die JPA-Abstraktion. Für komplexe
Auswertungen (FR-8) bleiben native Queries/Criteria möglich.

### Dokumentenablage

#### Alternative A: Dateisystem + Pfadreferenz in der DB
Skaliert bei großen Datenmengen besser und entlastet die DB. Nachteil: **zwei getrennte Backup- und
Konsistenzpfade** (DB + Dateisystem), kein transaktionaler Zusammenhang zwischen Datensatz und Datei,
zusätzliche Pfad-/Rechteverwaltung.

#### Alternative B: Objektspeicher (S3-kompatibel)
Cloud- bzw. zusätzlicher Dienst – **widerspricht ADR-006** (On-Premises, kein Internet, minimaler
Betrieb).

#### Alternative C: `bytea` in PostgreSQL (gewählt) ✅
Dokument und Metadaten liegen transaktional konsistent in **einer** Datenquelle; **ein einziger
Backup-Mechanismus** (ADR-005) deckt Stamm- und Dokumentdaten ab (siehe Abschnitt 6 der finalen
Architektur). Nachteil: DB-Wachstum – angesichts des Mengengerüsts (Abschnitt 8: ~2.000 Dokumente/Jahr
à 50–200 KB) unkritisch.

## Begründung

- **Sicherheit**: JPA/Prepared Statements reduzieren die im Alt-System vorhandene
  SQL-Injection-Anfälligkeit erheblich. Native und dynamisch konstruierte Abfragen bleiben
  prüfpflichtig.
- **Einfachheit im Betrieb**: `bytea` erspart einen zweiten Sicherungs- und Konsistenzpfad – passend
  zum Minimalbetrieb ohne IT-Personal (ADR-006).
- **Konsistenz**: Datensatz und zugehöriges Dokument werden in derselben Transaktion persistiert.

## Konsequenzen

### Positiv
- Parameterisierte Standardzugriffe und datenbankunabhängigere Geschäftslogik
- Einheitliches Repository-Modell, gute Testbarkeit (Mockito, Testcontainers)
- Genau **ein** Backup-/Restore-Pfad für Daten und Dokumente

### Negativ
- Hibernate-Abstraktion kann bei sehr komplexen Abfragen Feintuning erfordern (mitigiert durch
  native Queries/Criteria)
- `bytea` lässt die Datenbank schneller wachsen und belastet Backups stärker (technische Schuld R-6);
  bei künftig deutlich größeren Dokumentvolumina wäre Alternative A neu zu bewerten

### Neutral
- Große Binärinhalte werden per Streaming/`@Lob` gehandhabt, um Speicherlastspitzen zu vermeiden
