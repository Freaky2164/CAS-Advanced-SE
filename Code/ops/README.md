# ops/ – Betrieb und Werkzeuge

Dateien in diesem Verzeichnis gehören **nicht** zur Anwendung, sondern zu ihrem
Betrieb bzw. zur Entwicklungs-Werkzeugkette. Sie werden zur Laufzeit nie gelesen.

| Datei | Zweck |
|---|---|
| `sonar-analyse.ps1` | Startet die lokale SonarQube, erzeugt zur Laufzeit einen Analyse-Token, fährt die Test-DB hoch und ruft `mvnw verify sonar:sonar` auf. Siehe README, Abschnitt „Statische Analyse". |
| `verify-setup.sh` | **Linux/macOS:** prüft Build, JAR-Inhalt, Docker-Stack und Datenbank-Zustand (Flyway-Historie, App-Rolle, RLS, append-only). Braucht `docker`, `curl` und `jar` oder `unzip`. |
| `verify-setup.ps1` | **Windows:** dieselben Prüfungen als PowerShell-Variante. |
| `pgbackrest.conf` | Dokumentiertes Backup-Konzept (pgBackRest, Stanza `frauenhaus`). **Noch nicht in Betrieb** – die fehlenden Schritte stehen im Kopf der Datei. |

Beide `verify-setup`-Skripte sind inhaltlich gleich; wird eine Prüfung ergänzt, muss sie
in beiden Varianten nachgezogen werden. Siehe README, Abschnitt „Setup verifizieren".