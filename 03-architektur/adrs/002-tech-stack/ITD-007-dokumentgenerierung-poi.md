# Ersetzen der Windows COM Methoden und Klassen durch Apache POI
## Das Problem
Die aktuelle Lösung ist nicht betriebssystemagnostisch und Herstellerabhängig.

## Berücksichtigte Optionen

- Vorlagen auf .docx migrieren und mit docx4j/XDocReport befüllen
- **Original-.dot-Vorlagen über ihre Lesezeichen mit Apache POI ausfüllen; Versand per SMTP statt Outlook-COM**

## Begründung

Durch das Ausfüllen mit den Bookmarks können auch neue Versionen der Vorlagen problemlos befüllt werden und die Software ist nicht mehr abhängig von Microsoft sondern nutzt eine Open Source Lösung
## Anmerkungen
