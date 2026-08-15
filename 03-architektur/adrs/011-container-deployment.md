# ADR-011: Containerisiertes On-Premises-Deployment

## Status

**Akzeptiert und in `Code.zip` implementiert** – August 2026

Ersetzt die technische Ausprägung aus [ADR-006](006-deployment-model.md). Die
Standortentscheidung On-Premises bleibt bestehen.

## Kontext

ADR-006 sah Spring Boot und PostgreSQL als native Windows-Dienste vor. Der implementierte
Prototyp verwendet eine reproduzierbare Containerumgebung mit getrennten Komponenten.

## Entscheidung

Das System wird On-Premises mit Docker Compose betrieben:

- `backend`: Spring Boot mit Vaadin-UI und REST-API auf einer Java-25-JRE,
- `db`: PostgreSQL mit persistentem Volume und Initialisierungsskripten.

Das Backend veröffentlicht den konfigurierten Web-Port. TLS soll in Produktion durch einen
vorgelagerten Reverse Proxy terminiert werden; dieser ist nicht Bestandteil des Compose-Stacks.
Die Datenbank wird im Produktionsprofil nicht direkt am Host exponiert. Nachweise sind
`Code/docker-compose.yml` und `Code/Dockerfile` innerhalb von `Code.zip`.

## Konsequenzen

### Positiv

- reproduzierbares Deployment,
- klare Prozess- und Netzwerkgrenzen,
- einfachere Versionsbindung und Wiederherstellung der Laufzeitumgebung,
- konsistente Entwicklungs- und Produktionsartefakte.

### Negativ

- Docker wird zu einer zusätzlichen Betriebsabhängigkeit,
- Updates, Volumes, Zertifikate und Container-Logs benötigen ein Betriebskonzept,
- ein einzelner Host bleibt ein Single Point of Failure,
- On-Premises allein stellt keine DSGVO-Konformität her.

## Betriebsanforderungen

- Images versionieren und nicht ausschließlich `latest` verwenden,
- Datenbank-Volume und Backup-Repositories getrennt sichern,
- Patch- und Rollback-Prozess dokumentieren,
- TLS-Zertifikate überwachen und erneuern,
- Restore, Neustartverhalten und Recovery-Zeiten praktisch messen.
