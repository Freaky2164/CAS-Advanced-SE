# Rewrite des Backends zu Spring Boot

## Das Problem

Die Fachlogik der Software ist durch seine Swing-UI-Klassen und Windows Com Aufrufe nicht erweiter- und wartbar.

## Berücksichtigte Optionen

- Überführung des bestehenden Codes in einen neueren Java-Sprachstandard
- Kompletter Rewrite in einer anderen Sprache (C#, Python)
- **Neuimplementierung als Spring-Boot-Backend (Java 25, Maven, Schichtenarchitektur wie in 001 beschrieben)**

## Begründung

Die strukturellen Probleme waren durch Java Swing nicht beseitigbar, ohne ohnehin schon einen massiven Teil des Codes umzuschreiben, weshalb ein Rewrite keinen signifikanten Mehraufwand für einen großen Mehrwert bedeutet.
## Anmerkungen
