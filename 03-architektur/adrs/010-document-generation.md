# ADR-010: Dokumentgenerierung – Apache POI, docx4j/XDocReport (ohne Outlook)

## Status

**Akzeptiert** – Juli 2026

## Kontext

Die Erzeugung von Office-Dokumenten ist eine **fachlich differenzierende Kernfunktion** und war im
IST-System einer der aufwändigsten Bausteine. Gefordert sind:

- **FR-8 Auswertungen**: Excel-Reports (Spenden-, Bußgeld-, Vereinsstatistiken).
- **FR-9 Spendenbescheinigungen**: automatisiert, je Verein und Spendenart, auf Basis bestehender
  Word-Vorlagen (`backend/vorlagen-alt/`).
- **FR-10 Serienbriefe**: Massenanschreiben auf Grundlage von Kontakt-/Vereinsdaten.

Das Alt-System nutzte Word-Vorlagen (`.dot`) und **Outlook-COM/OLE-Automation**. Das neue Backend
läuft als **headless Windows-Dienst** (ADR-002/006) **ohne interaktive Office-/Outlook-Installation**.
Die Lösung muss daher rein serverseitig, ohne Office-Automation funktionieren, aber die vorhandenen
Vorlagen weiterverwenden können. ADR-002 hat Java bereits u. a. wegen des ausgereiften
Office-Ökosystems gewählt – dieses ADR konkretisiert die Bibliothekswahl.

## Entscheidung

- **Apache POI** (XSSF/SXSSF) für **Excel** (`.xlsx`, FR-8), inkl. Streaming für größere Exporte.
- **docx4j** bzw. **XDocReport** (mit Freemarker-/Velocity-Templating) für **Word** (`.docx`, FR-9/FR-10)
  auf Basis der bestehenden, nach `.docx` überführten Vorlagen.
- **Kein Outlook / keine Office-COM-Automation.** Die Dokumenterzeugung ist in einem eigenen Service
  gekapselt (`WordTemplateService` / `DocumentCreationService`, Abschnitt 4.5 der finalen
  Architektur; Template-Method/Strategy je Bescheinigungsart).

## Betrachtete Alternativen

### Alternative A: Fortführung Outlook-COM / Office-Automation (abgelehnt)
Bindet an eine lokale Office-/Outlook-Installation, ist im **headless Dienstbetrieb nicht stabil**
(interaktiver Kontext, Lizenz, Speicherlecks bei Automation) und zementiert die Fragilität des
Alt-Systems.

### Alternative B: JasperReports
Mächtige Report-Engine mit PDF-Fokus, aber **schwergewichtig** (Report-Design `.jrxml`, Laufzeit) und
für einfache vorlagenbasierte Serienbriefe überdimensioniert. Bleibt eine Option, falls später
umfangreiche PDF-Reports gefordert werden.

### Alternative C: LibreOffice headless (Konvertierung/Serverrendering)
Kann Vorlagen füllen und nach PDF konvertieren, führt aber eine **externe Prozess-/Dienst­abhängigkeit**
(LibreOffice-Installation) ein – zusätzlicher Betrieb entgegen dem Minimalprinzip (ADR-006).

### Alternative D: Reines String-/Text-Templating
Zu schwach für echte `.docx`-Formatierung und Serienbrief-Layouts (Tabellen, Formatvorlagen).

### Alternative E: Apache POI + docx4j/XDocReport (gewählt) ✅
Reine JVM-Bibliotheken, **headless serverfähig**, kein externer Prozess, direkte Weiterverwendung der
Vorlagen, ausgereiftes Open-Source-Ökosystem (Begründung konsistent mit ADR-002).

## Begründung

- **Headless-Tauglichkeit**: Passt zum Windows-Dienst-Betrieb ohne interaktive Office-Sitzung.
- **Vorlagenkontinuität**: Bestehende Word-Vorlagen bleiben nutzbar (docx4j/XDocReport), der fachliche
  Wissenstransfer aus dem Alt-System bleibt erhalten.
- **Kein zusätzlicher Betrieb**: Reine Bibliotheken statt externem Office-/Konvertierungsdienst.
- **Ökosystem-Konsistenz**: Bestätigt das in ADR-002 tragende Argument „bestes Office-Ökosystem in der
  JVM".

## Konsequenzen

### Positiv
- Vollständig serverseitige, reproduzierbare Dokumenterzeugung ohne Outlook
- Wiederverwendung vorhandener Vorlagen; Serienbrief-Batch ohne manuelle Office-Schritte
- Erzeugte Dokumente werden als `bytea` persistiert (ADR-008) und mitgesichert (ADR-005)

### Negativ
- `.dot`-Vorlagen des Alt-Systems müssen einmalig nach `.docx` überführt/normalisiert werden
- Große Serienbrief-Batches (FR-10) können rechen-/speicherintensiv sein – Streaming (SXSSF) bzw.
  chargenweise Verarbeitung vorsehen
- Zwei Word-Bibliotheken (docx4j/XDocReport) erhöhen die Abhängigkeitsfläche; die Auswahl je
  Anwendungsfall ist zu dokumentieren

### Neutral
- Für künftige umfangreiche PDF-Reports bleibt JasperReports (Alternative B) als Ergänzung offen

## Risiken

| Risiko | Wahrscheinlichkeit | Auswirkung | Gegenmaßnahme |
|--------|:------------------:|:----------:|---------------|
| Formatabweichungen bei der Vorlagenüberführung `.dot` → `.docx` | Mittel | Mittel | Stichproben-/Abnahmetest der Bescheinigungen im Cutover (Abschnitt 10.6) |
| Performance-Engpass bei großen Serienbrief-Batches | Niedrig | Mittel | Streaming/Chunking, asynchrone Verarbeitung |
