# ADR-004: Backup-Strategie – pgBackRest mit WAL-Archivierung und 3-2-1-Regel

## Status

**Akzeptiert** – Juni 2026

## Kontext

ADR-006 (Deployment-Modell) hält fest, dass die Backup-Strategie in einem separaten Dokument
zu definieren ist. NFR-4 fordert explizit:

> Daten müssen vollumfänglich und automatisiert gesichert werden und wiederherstellbar sein.
> Sicherungen müssen sicher und verschlüsselt sowie unter Einhaltung der DSGVO-Aufbewahrungsfristen
> abgelegt werden.

Das IST-System verfügt über keinerlei automatisierte Backup-Strategie: Ein manuelles Skript
(`frauenhaus_backup_jetzt.bat`) erstellt bei Bedarf ein einzelnes Vollbackup der MS-SQL-Server-
Datenbank, ausgelöst durch einen Mitarbeitenden per Doppelklick. Dies bringt erhebliche Risiken:

- **Kein Zeitplan**: Backups erfolgen nur, wenn sich jemand aktiv daran erinnert
- **Kein Point-in-Time-Recovery**: Zwischen zwei manuellen Backups gehen alle Änderungen
  bei einem Datenverlust unwiederbringlich verloren
- **Keine Verschlüsselung erkennbar**: Das `.bak`-Backup wird unverschlüsselt auf demselben
  Host abgelegt
- **Kein Offsite-Schutz**: Backup und Produktivdatenbank liegen auf demselben physischen
  System – Diebstahl, Feuer oder Wasserschaden zerstören beides gleichzeitig
- **Keine Restore-Tests**: Es ist nicht dokumentiert, ob ein Restore aus einem `.bak`-File
  jemals erfolgreich getestet wurde

Gemäß ADR-006 wird das System On-Premises betrieben, **ohne Internetzugang für den
Regelbetrieb**. Die Backup-Strategie muss diese Randbedingung respektieren: keine
Cloud-Speicherpflicht, kein permanenter Internetzugriff erforderlich.

## Entscheidung

Wir entscheiden uns für eine **automatisierte, verschlüsselte Backup-Strategie auf Basis von
pgBackRest** mit kontinuierlicher WAL-Archivierung (Point-in-Time-Recovery) und Einhaltung der
**3-2-1-Regel** (3 Kopien der Daten, 2 verschiedene Medien, 1 Kopie extern gelagert).

```
PostgreSQL (frauenhaus-Datenbank)
   │  archive_command (kontinuierliche WAL-Archivierung)
   ▼
pgBackRest ── repo1: lokales Backup-Volume (getrennt von pgdata), AES-256-verschlüsselt
   │            0 2 * * 0   → Vollbackup (sonntags)
   │            0 2 * * 1-6 → Differenzielles Backup (Mo–Sa)
   │            Retention: 4 Vollbackups, 14 Differenzen (~1 Monat PITR-Fenster)
   │
   └── repo2: externe Kopie (verschlüsselte USB-Medien, Rotation an zweitem Standort –
              kein Cloud-Ziel gemäß ADR-006)

Quartalsweiser Pflicht-Restore-Test → Staging-Container → Stichprobenvergleich → Protokoll
```

## Betrachtete Alternativen

### Alternative A: Status quo – manuelles Ad-hoc-Backup (Skript-Trigger)

Beibehaltung des bisherigen Ansatzes: ein Backup-Skript, das bei Bedarf manuell ausgeführt wird.

| Aspekt | Bewertung |
|--------|-----------|
| Implementierungsaufwand | ✅ Keiner – bereits vorhanden |
| Zuverlässigkeit | ❌ Abhängig von menschlicher Disziplin, keine Garantie für Regelmäßigkeit |
| Datenverlust-Fenster (RPO) | ❌ Undefiniert – im schlechtesten Fall Wochen |
| Point-in-Time-Recovery | ❌ Nicht möglich, nur Zeitpunkt des letzten manuellen Backups |
| Verschlüsselung | ❌ Nicht vorgesehen |
| Offsite-Kopie | ❌ Nicht vorgesehen |
| DSGVO-Aufbewahrungsfristen | ❌ Keine automatisierte Rotation/Löschung |

**Ablehnung**: Erfüllt NFR-4 ("vollumfänglich und automatisiert") nicht einmal ansatzweise.
Bereits die im Requirements-Dokument geforderte Automatisierung schließt manuelle
Einzel-Trigger-Lösungen aus.

### Alternative B: Geplantes logisches Backup mit `pg_dump` (Windows Task Scheduler)

Tägliches `pg_dump` der gesamten Datenbank, ausgelöst über die Windows-Aufgabenplanung,
Ablage als komprimiertes SQL-/Custom-Format-Backup.

| Aspekt | Bewertung |
|--------|-----------|
| Implementierungsaufwand | ✅ Gering – Standard-Tool, einfaches Scheduling |
| Automatisierung | ✅ Über Task Scheduler zuverlässig planbar |
| Point-in-Time-Recovery | ❌ Nur Zeitpunkt des letzten Dumps, keine WAL-basierte Feingranularität |
| Backup-Größe/-Dauer | ❌ Wächst linear mit Datenbankgröße, keine inkrementellen Backups |
| Restore-Geschwindigkeit | ⚠️ Vollständiger Restore aus SQL-Dump bei wachsender DB zunehmend langsam |
| Verschlüsselung | ⚠️ Muss manuell ergänzt werden (z.B. GPG-Verschlüsselung der Dump-Datei) |
| Konsistenz bei laufendem Betrieb | ✅ `pg_dump` erzeugt konsistente Snapshots (MVCC) |

**Nicht gewählt, aber als Fallback dokumentiert**: `pg_dump` ist deutlich besser als der
Status quo und für sehr kleine Datenbanken durchaus tragfähig. Da jedoch ohnehin eine
professionelle Lösung eingeführt werden soll und die Datenbank perspektivisch auch
Dokumente als `bytea` enthält (siehe Komponentendiagramm), ist eine Lösung mit
inkrementellen Backups und PITR vorzuziehen, ohne relevanten Mehraufwand.

### Alternative C: pgBackRest mit WAL-Archivierung (gewählt) ✅

| Aspekt | Bewertung |
|--------|-----------|
| Automatisierung | ✅ Cron-gesteuert, vollständig automatisiert (Voll-/Differenzbackup) |
| Point-in-Time-Recovery | ✅ Kontinuierliche WAL-Archivierung ermöglicht Wiederherstellung auf beliebigen Zeitpunkt |
| Backup-Größe | ✅ Differenzielle Backups sparen Speicherplatz gegenüber täglichem Vollbackup |
| Verschlüsselung | ✅ Native AES-256-Verschlüsselung (`repo1-cipher-type`), Passphrase getrennt verwahrt |
| Kompression | ✅ Eingebaut (`zst`), reduziert Speicherbedarf und Übertragungszeit |
| Retention-Management | ✅ Konfigurierbar (`repo1-retention-full`/`-diff`), unterstützt DSGVO-Aufbewahrungsfristen |
| 3-2-1-Fähigkeit | ✅ Unterstützt mehrere Repos (`repo1`, `repo2`) für lokale + externe Kopie |
| Community/Reife | ✅ De-facto-Standard-Tool im PostgreSQL-Ökosystem, aktiv gepflegt |
| Restore-Komplexität | ⚠️ Etwas höher als `pg_dump`-Restore, aber gut dokumentiert und scriptbar |

## Begründung

### 1. Erfüllung von NFR-4 in allen Teilaspekten

| NFR-4-Anforderung | Umsetzung durch pgBackRest |
|--------------------|------------------------------|
| Vollumfänglich | Physisches Backup der gesamten Datenbank inkl. aller Schemata (`frauenhaus.*`, `*_aud`-Audit-Tabellen, `app.app_user`) |
| Automatisiert | Cron-Zeitplan: Vollbackup sonntags 02:00, differenziell Mo–Sa 02:00 – kein manuelles Zutun |
| Wiederherstellbar | Point-in-Time-Recovery via WAL-Archiv, getestet via Pflicht-Restore-Test |
| Sicher & verschlüsselt | AES-256-Verschlüsselung (`repo1-cipher-type=aes-256-cbc`), Passphrase getrennt vom Backup verwahrt |
| DSGVO-Aufbewahrungsfristen | Retention-Policy (4 Voll-/14 Differenzbackups ≈ 1 Monat) konfigurierbar an rechtliche Löschfristen anpassbar |

### 2. Ein Backup-Pfad für alle Daten

Dokumente (Spendenbescheinigungen, Serienbriefe, Anhänge) werden als `bytea` direkt in der
PostgreSQL-Datenbank gespeichert (siehe Komponentendiagramm, `DokumentRepository`/`Dokument`-
Entity) statt im Dateisystem. Dadurch deckt **ein einziger Backup-Mechanismus** sowohl
Stammdaten als auch Dokumente ab – es gibt keine zweite, separat zu pflegende
Dateisystem-Backup-Pipeline mit eigenem Zeitplan und eigenem Risiko der Inkonsistenz
zwischen DB-Stand und Dateisystem-Stand.

### 3. 3-2-1-Regel ohne Cloud-Abhängigkeit

Konsistent mit ADR-006 (kein Internetzugang im Regelbetrieb, keine Cloud-Speicherung
sensibler Daten) wird die externe Kopie (`repo2`) **nicht** auf einem Cloud-Speicher
abgelegt, sondern auf verschlüsselten USB-Datenträgern realisiert, die im Rotationsprinzip
an einem zweiten physischen Standort gelagert werden:

- **3 Kopien**: Produktivdatenbank + lokales `repo1`-Backup + externes `repo2`-Backup
- **2 Medien**: internes Backup-Volume (getrennt von `pgdata`) + externe USB-Medien
- **1 extern**: USB-Rotation an einem zweiten Standort schützt vor Totalverlust durch
  Feuer, Wasserschaden oder Diebstahl des Servers

Die in der pgBackRest-Dokumentation vorgesehene Option, `repo2` auf einen
S3-kompatiblen Speicher zu spiegeln, wird bewusst **nicht** genutzt, um die
Konsistenz mit der On-Premises-Entscheidung (ADR-006) zu wahren.

### 4. Schlüsselverwaltung getrennt vom Backup

Die Verschlüsselungs-Passphrase (`repo1-cipher-pass`) wird explizit **nicht** im
Backup-Verzeichnis oder Repository abgelegt, sondern separat verwahrt (Passwort-Manager
oder physischer Safe). Ein gestohlenes verschlüsseltes Backup ist ohne Passphrase wertlos;
eine im selben Verzeichnis liegende Passphrase würde die Verschlüsselung hingegen
wirkungslos machen.

### 5. Pflicht zum regelmäßigen Restore-Test

Ein Backup, dessen Wiederherstellung nie getestet wurde, ist kein verlässliches Backup.
Die Strategie schreibt einen **quartalsweisen Restore-Test** in einer isolierten
Staging-Umgebung vor: Restore in einen leeren Container, Start der Anwendung gegen das
wiederhergestellte Backup, Stichprobenvergleich (z.B. Mitgliederzahl, Spenden-/
Bußgeldsummen des Vorjahres) und Protokollierung von Datum, Testperson und Ergebnis.
Dies deckt Fehlkonfigurationen auf, bevor sie im Ernstfall entdeckt werden.

### 6. Verhältnismäßiger Werkzeugeinsatz

pgBackRest ist speziell für PostgreSQL entwickelt und im Gegensatz zu generischen
Dateisystem-Backup-Tools in der Lage, konsistente Backups **während des laufenden Betriebs**
zu erstellen, ohne die Anwendung zu pausieren. Es ist etabliertes Open-Source-Werkzeug ohne
Lizenzkosten – passend zum Gesamtbudget-Rahmen eines spendenfinanzierten Vereins.

## Risiken und Mitigationen

| Risiko | Wahrscheinlichkeit | Auswirkung | Mitigation |
|--------|--------------------|-----------|-------------|
| USB-Rotation wird organisatorisch vergessen | Mittel | Hoch | Feste Verantwortlichkeit (z.B. Verwaltungskraft), Erinnerungsroutine, Protokoll der letzten Rotation |
| Verlust der Verschlüsselungs-Passphrase | Niedrig | Sehr hoch (Backup unbrauchbar) | Passphrase an zwei getrennten, sicheren Orten hinterlegt (z.B. Safe + Passwort-Manager eines zweiten Verantwortlichen) |
| Restore-Test wird nicht durchgeführt | Mittel | Hoch (unentdeckte Backup-Fehler) | Fester Kalendereintrag, Protokolltabelle im Betriebsdokument als Nachweis |
| Backup-Volume liegt faktisch doch auf derselben Platte wie `pgdata` | Niedrig | Hoch | Explizite Konfigurationsprüfung bei Inbetriebnahme (getrenntes Volume/Partition) |
| WAL-Archiv läuft bei Speicherplatzmangel voll | Niedrig | Mittel | Monitoring des Backup-Volumes, Retention-Policy begrenzt Wachstum |

## Konsequenzen

### Positiv
- Erfüllt NFR-4 vollständig: automatisiert, verschlüsselt, wiederherstellbar,
  mit konfigurierbarer Aufbewahrungsfrist
- Point-in-Time-Recovery ermöglicht Wiederherstellung auf einen beliebigen Zeitpunkt
  (z.B. unmittelbar vor einer versehentlichen Löschung), nicht nur auf den letzten Backup-Tag
- Ein einheitlicher Backup-Pfad für Stammdaten und Dokumente (beide in PostgreSQL, kein
  separates Dateisystem-Backup nötig)
- Konsistent mit der On-Premises-/DSGVO-Entscheidung aus ADR-006 – keine Cloud-Abhängigkeit
- Deutlich geringeres Datenverlustrisiko (RPO: max. 1 Tag) gegenüber dem manuellen
  IST-Zustand (RPO: undefiniert, potenziell Wochen)

### Negativ
- Höhere initiale Einrichtungskomplexität als ein einfaches `pg_dump`-Skript
  (Stanza-Konfiguration, WAL-Archivierung, Verschlüsselungs-Setup)
- USB-Rotation für die Offsite-Kopie erfordert weiterhin organisatorische Disziplin
  (kein vollautomatischer Offsite-Transport ohne Internetanbindung möglich)
- Quartalsweise Restore-Tests verursachen wiederkehrenden manuellen Aufwand

### Neutral
- Konkrete Cron-Zeitpläne und Retention-Werte (4 Voll-/14 Differenzbackups) können bei
  Bedarf an tatsächliche Datenmengen und Änderungsraten angepasst werden
- Die Backup-Konfiguration (`pgbackrest.conf`) und das Betriebsdokument (inkl.
  Restore-Anleitung und Testprotokoll) werden im Repository unter `backend/ops/` gepflegt
- Eine spätere Migration von `repo2` auf ein anderes externes Medium (z.B. NAS am
  Zweitstandort) ist ohne Änderung der Kernstrategie möglich
