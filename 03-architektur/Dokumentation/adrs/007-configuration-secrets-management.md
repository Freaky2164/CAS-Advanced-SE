# ADR-007: Konfigurations- & Secrets-Management – externalisierte Konfiguration mit zugriffsgeschützten Geheimnissen

## Status

**Akzeptiert** – Juli 2026

## Kontext

Der zentrale Anlass der Modernisierung war eine gravierende Sicherheitslücke des IST-Systems
(ADR-001): Die Datenbank-Zugangsdaten lagen **im Klartext** in lokalen `.ini`-Dateien auf jedem
Client-PC. Die neue, zentral betriebene Anwendung führt mehrere sicherheitskritische Geheimnisse
an **einer** Stelle zusammen, die geschützt verwaltet werden müssen:

| Geheimnis | Herkunft / Zweck |
|-----------|------------------|
| PostgreSQL-Zugangsdaten | Backend → Datenbank (JDBC) |
| Backup-Passphrase (AES-256) | pgBackRest-Verschlüsselung (ADR-005) |
| TLS-Keystore-Passwort | HTTPS-Serverzertifikat (Abschnitt 10.2 der finalen Architektur) |
| Initiales Administrator-Passwort | Erst-Bootstrap (`AdminBootstrap`, ADR-004) |
| Optionaler BCrypt-Pepper | zusätzliche Härtung des Passwort-Hashings (ADR-004) |

**Randbedingungen** (ADR-006, Abschnitt 1.3): On-Premises-Betrieb auf **einem** Windows-Server,
kein Internetzugang, kein Cloud-Secret-Manager, kleiner Verein **ohne IT-Personal**. Die Lösung
muss betreibbar bleiben, ohne zusätzliche Infrastruktur einzuführen.

## Entscheidung

Wir entscheiden uns für **externalisierte Konfiguration mit dateisystem-geschützten Geheimnissen**:

1. **Keine Geheimnisse im Versionskontrollsystem und nicht im Fat-JAR.** Das Repository enthält
   nur Platzhalter/Beispielwerte (`application.properties` mit `${...}`-Verweisen).
2. **Externe Konfigurationsdatei** außerhalb des JARs (z. B. `config/application.properties` neben
   dem Dienst), eingebunden über Spring Boots `spring.config.additional-location`. Die Datei ist per
   **NTFS-ACL ausschließlich für das Windows-Dienstkonto** les-/schreibbar.
3. **Sensible Werte** werden bevorzugt über **Umgebungsvariablen des Dienstkontos**
   (bzw. `SPRING_APPLICATION_JSON`) injiziert; für Werte, die in der Datei stehen müssen, werden
   diese mit **Jasypt** verschlüsselt und der Master-Key als Umgebungsvariable des Dienstkontos
   gehalten.
4. **Kein hartcodiertes Standardpasswort.** `AdminBootstrap` erzwingt beim ersten Start das Setzen
   eines individuellen Administrator-Passworts (BCrypt-Hash), analog ADR-004.
5. Die **Backup-Passphrase** wird gemäß ADR-005 organisatorisch **getrennt** von den Backups
   verwahrt.

## Betrachtete Alternativen

### Alternative A: Klartext-Werte in gebündelter `application.properties` (abgelehnt)

Einfach, aber wiederholt exakt den Fehler des IST-Systems (Klartext-Credentials) und legt
Geheimnisse potenziell im Repository/JAR ab. Nicht akzeptabel.

### Alternative B: Windows Credential Manager / DPAPI

Betriebssystemnahe, verschlüsselte Ablage. Technisch solide, aber an einen Benutzer-/Maschinen­kontext
gebunden, schlechter unter Versionskontrolle/Dokumentation nachvollziehbar und für ein Team ohne
IT-Personal aufwändiger einzurichten und zu übertragen (z. B. bei Serverumzug).

### Alternative C: Externe Konfiguration + NTFS-ACL (+ optional Jasypt) (gewählt) ✅

Nutzt Spring-Boot-Bordmittel (externalisierte Konfiguration, Property-Platzhalter), erzwingt die
Trennung von Code und Geheimnis und schützt die Datei über Dateisystemrechte des Dienstkontos.
Optionale Jasypt-Verschlüsselung härtet ruhende Werte zusätzlich – ohne neue Infrastruktur.

### Alternative D: Externer Secret-Manager (HashiCorp Vault, Cloud KMS)

Stand der Technik für größere Landschaften, aber **überdimensioniert**: eigener Dienst mit Betrieb,
Verfügbarkeit und Know-how – widerspricht dem On-Premises-Minimalbetrieb (ADR-006) eines kleinen
Vereins.

## Begründung

- Die Entscheidung adressiert unmittelbar die **Kernschwachstelle** des Alt-Systems (Klartext-Credentials).
- Sie kommt **ohne zusätzliche Infrastruktur** aus (nur Spring-Boot-Bordmittel + Dateisystemrechte)
  und bleibt damit im Rahmen von ADR-006 und der Randbedingung „kein IT-Personal".
- Sie ist **schichtenkonform**: Kein Anwendungscode kennt Geheimnisse fest, alle Werte kommen aus der
  externen Konfiguration.

## Konsequenzen

### Positiv
- Keine Klartext-Geheimnisse in Code, Repository oder JAR
- Zugriff auf die Konfigurationsdatei auf das Dienstkonto beschränkt (NTFS-ACL)
- Trennung von Deployment-Artefakt (JAR) und Umgebungskonfiguration erleichtert Umzüge/Updates

### Negativ
- Betrieb muss ACLs und Umgebungsvariablen korrekt setzen (organisatorische Sorgfalt nötig)
- Jasypt-Master-Key ist selbst ein zu schützendes Geheimnis (Bootstrapping-Problem, bewusst
  akzeptiert: reduziert von *n* Geheimnissen auf einen geschützten Schlüssel)

### Neutral
- Bei einem späteren Wechsel auf externe Erreichbarkeit oder mehrere Server (Abweichung von ADR-006)
  wäre ein zentraler Secret-Manager (Alternative D) neu zu bewerten

## Risiken

| Risiko | Wahrscheinlichkeit | Auswirkung | Gegenmaßnahme |
|--------|:------------------:|:----------:|---------------|
| Fehlkonfigurierte Dateirechte legen Geheimnisse offen | Niedrig | Hoch | ACL-Prüfung als Teil der Inbetriebnahme-Checkliste (Abschnitt 10.6) |
| Verlust des Jasypt-Master-Keys / der Backup-Passphrase | Niedrig | Hoch | Getrennte, dokumentierte Verwahrung (Passwort-Tresor des Vereins) |
| Geheimnis versehentlich committet | Niedrig | Hoch | `.gitignore` für Konfig-Dateien, nur Platzhalter im Repo |
