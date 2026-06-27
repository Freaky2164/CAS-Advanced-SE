# FH_MA – Projekt- und Seminararbeit

Dieses Repository enthält das Basis-Projekt „FH_MA“ für das Modul **CSC1200**
(Prof. Dr. Holger D. Hofmann) im M.Sc. Informatik. Die Ergebnisse der
Projektarbeit dienen als Grundlage für die anzufertigende Seminararbeit.

---

## 📌 Rahmenbedingungen & Einreichung

- **Plattform:** Die gesamte Software inkl. Datenbank muss unter **Microsoft
  Windows** lauffähig sein.
- **Teamarbeit:** Alle Aufgaben sind (soweit nicht anders angegeben)
  Teamaufgaben. Auch Misserfolge („Dinge, die nicht funktioniert haben“) sind
  wertvolle Erkenntnisse und sollen präsentiert werden.
- **Software-Prozess:** Neben den Arbeitsergebnissen muss auch die methodische
  Herangehensweise dokumentiert werden.
- **Autorenschaft:** Die Urheberschaft muss jederzeit nachvollziehbar sein:
  - In der Dokumentation: Kenntlichmachung über die **Kopfzeile**.
  - Im Quelltext: Kenntlichmachung über die `@author`-Annotation.

### Abgabe (zum Ende der 2. Theoriephase)

1. **Artefakte (Dateien):**
   - **Gesamtdokumentation (PDF):** Aufgebaut wie eine Seminararbeit (korrekte
     Form, Struktur, Zitierstil, Literaturverzeichnis).
   - **Quelltext (ZIP):** Der vollständig entwickelte Source Code.
   - **Mitarbeiter-Archiv (ZIP):** Eine ZIP-Datei mit jeweils einem eigenen
     Ordner pro ProjektmitarbeiterIn. Dieser Ordner muss alle individuell
     erstellten Artefakte (Dokumentation, INI-Dateien, Java-Klassen, JARs etc.)
     enthalten.
2. **Präsentation:** Eine ca. **30- bis 45-minütige Präsentation** pro Gruppe
   zur Vorstellung der Arbeitsergebnisse.

---

## 📂 Gruppen & Aufgabenstellungen

### Gruppe 1: IST-Zustand

- **Dokumentation:** Analyse und Dokumentation des aktuellen Software-Standes
  (Software-Komponenten, Installation, Konfiguration, Architektur). Ziel: Ein
  Informatik-Studierender im 2. Semester muss die Software danach
  nachvollziehen, installieren und starten können.
- **Metriken:** In Zusammenarbeit mit Gruppe 6 Metriken für den aktuellen Stand
  (inkl. Datenbank) festlegen, anwenden und interpretieren.

### Gruppe 2: Datenbank

- **Evaluation:** Prüfen, ob der aktuell genutzte _Microsoft SQL Server Express_
  durch MySQL oder eine andere Datenbank ersetzt werden kann. Bei der Begründung
  ist die aktuelle Verwendung von _Stored Procedures_ zwingend zu
  berücksichtigen.
- **Schema-Optimierung:** Die aktuelle Datenbankstruktur analysieren (Gibt es
  ungenutzte Tabellen?). Ein neues Datenbankschema entwerfen und die Vorteile
  gegenüber dem alten Schema begründen.

### Gruppe 3: Architektur-Entwurf

- **Neukonzeption:** Eine neue Architektur entwerfen und dokumentieren.
  Besonderer Wert liegt auf:
  - Datensicherheit
  - Backup & Recovery
  - Einfacher Wartung des Systems
- **Umsetzung & Evaluation:** Die neue Architektur im Gesamtteam umsetzen. In
  Zusammenarbeit mit Gruppe 6 die initialen Metriken auf die neue Software
  anwenden und interpretieren.

### Gruppe 4: GUI 2.0

- **Konzept:** Ein neues Konzept für die Benutzeroberfläche entwerfen und die
  getroffenen Entwurfsentscheidungen ausführlich erläutern.
- **Umsetzung:** Die neue GUI (prototypisch) in der Software implementieren.

### Gruppe 5: Vorlagen

- **Modifikation:** Die alten Vorlagen (`Vorlage alt*.pdf` im Ordner
  _Vorlagen_neu_) basierend auf den Vorgaben des Finanzministeriums
  (`Finanzministerium_Vorlage neu.pdf`) modifizieren.
- **Dokumentation:** Den Prozess der Vorlagenanpassung lückenlos dokumentieren.

### Gruppe 6: Metriken

- **Definition:** Geeignete Metriken zur Qualitätsbeurteilung des Projekts
  suchen (insb. objektorientierte Metriken). Zusätzlich eigene, aggregierte
  Metriken für dieses spezielle Projekt erstellen und anwenden.
- **Werkzeuge:** Es darf zur Erhebung **ausschließlich Open Source-Software**
  verwendet werden.
