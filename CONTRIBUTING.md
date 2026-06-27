# Contributing Guidelines

Um das Projekt übersichtlich zu halten und Konflikte zu vermeiden, gelten für
alle Mitwirkenden diese zwei grundlegenden Regeln:

## 1. Git LFS (Large File Storage) ist Pflicht

Alle Binärdateien (z. B. PDFs, ZIP-Archive, JAR-Dateien, Bilder) **müssen**
zwingend über Git LFS verwaltet werden. Lade solche Dateien niemals normal ins
Git hoch!

- **Einmalig aktivieren:** `git lfs install`
- **Dateien tracken (Beispiel):** `git lfs track "*.pdf"`

## 2. Pull Request und Branch Convention

Es wäre super, wenn wir einen klaren tag für unsere Aufgabe verwenden e.g.
`faet/architektur/adr-1-baseline`
