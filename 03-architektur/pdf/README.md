# PDF-Toolchain (containerisiert)

Erzeugt aus der Markdown-Seminararbeit ein PDF – **ohne lokale Installation** von
Pandoc, LaTeX oder Node. Alles läuft in einem Docker-Container.

## Pipeline

```
seminararbeit-architektur.md
        │  Pandoc  (Markdown -> HTML, TOC, nummerierte Abschnitte)
        ▼
   render.html  (+ Mermaid.js, style.css)
        │  Chromium (Vorab-PDF und gerendertes HTML)
        ▼
   Seitenzuordnung für Inhalts-, Abbildungs-, Tabellen- und Listingverzeichnis
        │  Chromium (finales PDF)
        ▼
seminararbeit-architektur.pdf
```

Der HTML-→-Chromium-Weg wurde bewusst gewählt (statt LaTeX), weil das Dokument
**Mermaid-Diagramme**, **Emojis** (✅ ⚠️ ★) und **Umlaute** enthält – Chromium
rendert all das zuverlässig mit den mitgelieferten Noto-/DejaVu-Schriften.

Die PDF enthält auf jeder Seite genau einen verantwortlichen Autor in der Kopfzeile.
Die Zuordnung erfolgt automatisch anhand der Hauptkapitel:

| Kapitelblock | Autor |
|--------------|-------|
| Frontmatter und Kapitel 1–3 | Nils Firschau |
| Kapitel 4–5 | Paul Faller |
| Kapitel 6–8 | Robin Steiner |
| Kapitel 9, Fazit und Anhänge | Ole Schildt |

## Voraussetzung

Nur **Docker** (Docker Desktop unter Windows muss laufen). Sonst nichts.

## Verwendung

Aus dem Ordner `03-architektur` (oder mit vollem Pfad):

**Windows (PowerShell):**
```powershell
.\pdf\build-pdf.ps1
```

**Linux/macOS:**
```bash
./pdf/build-pdf.sh
```

Das Skript baut beim ersten Lauf das Image `fhma-seminar-pdf` und erzeugt danach
`seminararbeit-architektur.pdf` neben der Markdown-Datei. Folgeläufe nutzen das
gecachte Image (schnell). Neu bauen erzwingen: `-Rebuild` (PowerShell).

Andere Datei rendern:
```powershell
.\pdf\build-pdf.ps1 -Input 02-finale-architektur.md -Output finale-architektur.pdf
```

## Manuell (ohne Wrapper-Skript)

```bash
docker build -t fhma-seminar-pdf ./pdf
docker run --rm -v "${PWD}:/work" fhma-seminar-pdf seminararbeit-architektur.md seminararbeit-architektur.pdf
```

Das aktuelle Verzeichnis wird nach `/work` gemountet; Ein-/Ausgabepfade sind
relativ zu diesem Mount.

## Dateien

| Datei | Zweck |
|-------|-------|
| `Dockerfile` | Image mit Pandoc, Chromium, Node, Mermaid, Schriften |
| `md2pdf.sh` | Entrypoint: Pandoc- und Chromium-Schritt |
| `template.html` | Pandoc-HTML-Template inkl. Mermaid-Initialisierung |
| `style.css` | A4-Druck-Stylesheet (Titelseite, Tabellen, Diagramme) |
| `render.js` | Puppeteer-Skript (wartet auf Mermaid und druckt das A4-PDF) |
| `add_directory_page_numbers.py` | Zweiter Durchlauf für Seitenzahlen in allen Verzeichnissen |
| `build-pdf.ps1` / `build-pdf.sh` | Komfort-Wrapper (Image bauen + Container ausführen) |
