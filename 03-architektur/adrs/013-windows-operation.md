# ADR-013: Windows-konformer Betrieb des Container-Stacks

## Status

**Akzeptiert** – August 2026

Präzisiert die Laufzeitausprägung aus [ADR-011](011-container-deployment.md) im Hinblick auf die
verbindliche Rahmenbedingung eines vollständigen Windows-Betriebs. Steht in direktem Zusammenhang
mit der DBMS-Entscheidung [ADR-014](014-database-management-system.md) und der
Backup-Strategie [ADR-005](005-backup-strategy.md).

## Kontext

Die Aufgabenstellung legt als verbindliche Rahmenbedingung fest:

> Die ganze Software inkl. Datenbank soll unter Microsoft Windows laufen.

Der in ADR-011 gewählte und im Prototyp umgesetzte Stack besteht aus zwei Linux-Containern
(Spring-Boot-Backend und PostgreSQL) unter Docker Compose. PostgreSQL wird über ein
Linux-Basisimage betrieben, und das vorgesehene Backupwerkzeug pgBackRest besitzt keine native
Windows-Installation. Ohne explizite Entscheidung könnte dieser Stack als Verstoß gegen die
Windows-Vorgabe missverstanden werden. Die Frage, **wie** der Container-Stack Windows-konform
betrieben wird, muss daher als eigenständige Architekturentscheidung dokumentiert und gegen
native Alternativen abgewogen werden.

## Entscheidung

Das Backend und die Datenbank werden als Container unter **Docker Desktop bzw. Docker Engine auf
einem Windows-Host** ausgeführt. Die dafür benötigte Linux-Laufzeit stellt das in Windows
integrierte **WSL 2** (Windows Subsystem for Linux 2) bereit.

Aus Sicht des Betreibers sind sowohl der Anwendungsserver als auch die Datenbank damit ein unter
Windows verwalteter, über den Windows-Diensthost gestarteter Prozess. Host, Speicher, Netzwerk und
Betriebsführung unterstehen vollständig Windows; die Container isolieren lediglich die
Laufzeitumgebung. Die Rahmenbedingung „gesamte Software inklusive Datenbank unter Microsoft
Windows" ist damit erfüllt.

## Betrachtete Alternativen

| Betriebsvariante | Windows-Konformität | Wartung/Support beim Verein | Backup-Konsequenz | Bewertung |
|------------------|---------------------|-----------------------------|-------------------|-----------|
| Docker Desktop bzw. Docker Engine mit WSL 2 | läuft als verwalteter Dienst auf einem Windows-Host | eine reproduzierbare, versionierte Laufzeit; geringe Wartung | pgBackRest im Linux-Container nutzbar; als Übergang `pg_dump` vom Windows-Host | **gewählt** |
| Windows-Container | nativ Windows | offizielle PostgreSQL-Windows-Container werden kaum gepflegt | eingeschränkte Werkzeugunterstützung | verworfen |
| Nativer Windows-Dienst (PostgreSQL-Dienst + Java-Dienst) | vollständig nativ | vertrauter, aber manueller Betrieb je Komponente | pgBackRest nicht nativ; Umstieg auf `pg_basebackup`/`pg_dump` nötig | dokumentierte Rückfalloption |
| Hyper-V-Linux-VM auf Windows-Host | Windows-Host, Linux-Gast | zusätzliche VM-Administration | pgBackRest nutzbar, höherer Betriebsaufwand | verworfen |

### Windows-Container

Windows-Container würden die Container-Vorteile bei formal nativer Windows-Ausführung erhalten.
Für PostgreSQL existieren jedoch keine offiziell gepflegten Windows-Container-Images; die
verfügbaren Community-Images sind für einen Produktivbetrieb mit sensiblen Daten nicht ausreichend
gestützt. Auch die Werkzeugunterstützung (Backup, Monitoring) ist deutlich schwächer.

### Nativer Windows-Dienst (Rückfalloption)

PostgreSQL wird offiziell als Windows-Dienst bereitgestellt, und die Spring-Boot-Anwendung kann
als eigenständiger Java-Dienst laufen. Diese Variante ist am vertrautesten, erfordert jedoch die
manuelle Einrichtung und Pflege jeder Komponente und verliert die reproduzierbare, versionierte
Laufzeit. Vor allem müsste die Backupstrategie umgestellt werden, da pgBackRest Windows nicht
nativ unterstützt (Umstieg auf `pg_basebackup` oder `pg_dump` mit WAL-Archivierung, siehe
ADR-005). Diese Option bleibt als dokumentierte Rückfalloption bestehen, falls eine spätere
Ausschreibung eine Container-freie Installation verlangt.

### Hyper-V-Linux-VM

Eine dedizierte Linux-VM auf dem Windows-Host würde pgBackRest ermöglichen, verlagert den Betrieb
aber faktisch in ein separat zu administrierendes Linux-System und erhöht den Betriebsaufwand
gegenüber der WSL-2-Variante, ohne einen zusätzlichen Vorteil zu bieten.

## Begründung

- Die containerisierte Variante liefert reproduzierbare, versionierte Umgebungen, einheitliche
  Konfiguration und eine einfachere Wartung (vgl. ADR-011) – dies unterstützt die geforderte
  einfache Wartbarkeit am besten.
- WSL 2 ist Bestandteil von Windows; es entsteht keine vom Windows-Betrieb losgelöste
  Fremdplattform. Der Diensthost, die Datenträger und das Netzwerk bleiben unter Windows-Kontrolle.
- Das vorgesehene Backupwerkzeug pgBackRest besitzt keine native Windows-Installation und ist im
  Linux-Container ohne Einschränkungen nutzbar.
- Die native Alternative bleibt vollständig dokumentiert und kann ohne Änderung der fachlichen
  Architektur aktiviert werden.

## Konsequenzen

### Positiv

- Erfüllt die verbindliche Windows-Rahmenbedingung, ohne die Vorteile des Container-Deployments
  aufzugeben.
- Betrieb, Speicher und Netzwerk verbleiben vollständig unter Windows-Verwaltung.
- pgBackRest bleibt als Ziel-Backupwerkzeug einsetzbar.

### Negativ

- Docker Desktop bzw. Docker Engine und WSL 2 sind zusätzliche Betriebsabhängigkeiten, die
  aktualisiert und überwacht werden müssen.
- Der Verein benötigt Grundkenntnisse im Umgang mit Docker/WSL 2 oder einen entsprechenden
  Dienstleister.
- Die Windows-Konformität ist eine Betriebs- und keine reine Installationsentscheidung; sie muss
  bei Systemänderungen erneut geprüft werden.

### Neutral

- Für den Übergang bis zur produktiven pgBackRest-Integration wird das Backup über einen
  Windows-konformen `pg_dump`-Job vom Windows-Host ausgeführt (siehe ADR-005 und §7.4 der
  Seminararbeit).
- Bei geänderten Ausschreibungsbedingungen ist der Wechsel auf den nativen Windows-Dienst als
  Rückfalloption vorbereitet.
