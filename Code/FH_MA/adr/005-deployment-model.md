# ADR-005: Deployment-Modell – On-Premises statt Cloud (DSGVO)

## Status

**Akzeptiert** – Juni 2026

## Kontext

Gemäß ADR-001 wird das System als 3-Schichten-Architektur mit zentralem Backend und
SQL-Server-Datenbank betrieben. Es muss entschieden werden, **wo** diese Infrastruktur
physisch betrieben wird: auf einem lokalen Server im Frauenhaus oder
bei einem Cloud-Anbieter.

Die Entscheidung wird maßgeblich durch folgende Rahmenbedingungen beeinflusst:

| Rahmenbedingung | Beschreibung |
|-----------------|-------------|
| **Datenklassifikation** | Höchst sensible personenbezogene Daten (Schutz suchende Frauen und Kinder, Adressen, Kontaktdaten, Falldokumentation) – besondere Kategorie nach Art. 9 DSGVO |
| **Schutzbedarf** | Sehr hoch – ein Datenleck kann Leib und Leben gefährden (Stalking, häusliche Gewalt) |
| **Organisationsform** | Kleiner Verein, kein IT-Personal, kein dedizierter Datenschutzbeauftragter |
| **Nutzeranzahl** | Geringe Anzahl gleichzeitiger Arbeitsplätze im lokalen Netzwerk |
| **Budget** | Begrenzt – keine laufenden Cloud-Kosten tragbar |
| **Verfügbarkeit** | Generelle Bürozeiten (Mo–Fr), kein 24/7-Betrieb erforderlich |
| **Regulatorik** | DSGVO und ggf. Landesdatenschutzgesetz |

## Entscheidung

Wir entscheiden uns für **On-Premises-Deployment** auf einem dedizierten Windows-Server
im lokalen Netzwerk des Frauenhauses. Das gesamte System (Backend-Dienst + SQL Server +
Backups) verbleibt physisch in den örtlichen Räumlichkeiten.

```
[Vereinsbüro – physisch geschützter Raum]
┌──────────────────────────────────────────────┐
│  Windows Server            │
│  ├── Spring Boot Backend (WinSW-Dienst)     │
│  ├── MS SQL Server                          │
│  └── Verschlüsselte Backups (lokal + USB)   │
└──────────────────────────────────────────────┘
         ▲ LAN (kein Internet-Zugang nötig)
         │
   ┌─────┴─────┐
   │ Clients   │  (Browser im LAN)
   └───────────┘
```

## Betrachtete Alternativen

### Alternative A: Public Cloud (Azure / AWS / Hetzner)

Hosting des Backends und der Datenbank bei einem Cloud-Anbieter (z.B. Azure App Service +
Azure SQL, AWS EC2 + RDS, oder Hetzner Cloud).

| Aspekt | Bewertung |
|--------|-----------|
| **Verfügbarkeit** | ✅ 99,9% SLA, automatische Redundanz |
| **Skalierbarkeit** | ✅ Ressourcen bei Bedarf anpassbar |
| **Managed Services** | ✅ Automatische Patches, Backups, Monitoring |
| **Zugriff von extern** | ✅ Zugriff von überall möglich (Home-Office) |
| **DSGVO-Konformität** | ⚠️ Auftragsverarbeitungsvertrag (AVV) erforderlich, Serverstandort muss EU sein |
| **Datensouveränität** | ❌ Daten verlassen physische Kontrolle des Vereins |
| **Kosten** | ❌ Laufende monatliche Kosten (VM + DB + Backup) |
| **Komplexität** | ⚠️ Netzwerkkonfiguration, Firewall-Regeln, VPN für sicheren Zugriff |
| **Abhängigkeit** | ❌ Internet-Ausfall = kein Zugriff auf das System |
| **Angriffsfläche** | ❌ System über Internet erreichbar – DDoS, Brute-Force, Exploits |

**Ablehnung**: Für höchst sensible Daten (Schutz suchender Frauen) ist die Übertragung
der Datenhoheit an einen Cloud-Anbieter ein unverhältnismäßiges Risiko. Die US-Cloud-Act-
Problematik bei Azure/AWS ist nach dem Schrems-II-Urteil des EuGH (2020) juristisch ungeklärt.
Selbst bei EU-Anbietern (Hetzner) verlässt der Verein seine physische Datenhoheit – bei
einem Datenleck wären die Konsequenzen existenzbedrohend (Gefährdung von Menschenleben).
Zusätzlich sind die laufenden Kosten für einen kleinen Verein nicht tragbar.

### Alternative B: Hybrid-Ansatz (lokaler Server + Cloud-Backup)

Primärsystem lokal, aber verschlüsselte Backups in der Cloud (z.B. verschlüsselt auf
einem EU-Speicherdienst).

| Aspekt | Bewertung |
|--------|-----------|
| **Datenhoheit (Primärsystem)** | ✅ Daten bleiben lokal |
| **Backup-Sicherheit** | ⚠️ Verschlüsselte Backups in Cloud – Schlüssel bleibt lokal |
| **Disaster Recovery** | ✅ Offsite-Backup schützt vor Feuer/Diebstahl |
| **DSGVO** | ⚠️ Auch verschlüsselte Backups sind personenbezogene Daten – AVV nötig |
| **Kosten** | ⚠️ Gering , aber dennoch laufend |
| **Komplexität** | ⚠️ Automatisierte Backup-Pipeline muss eingerichtet werden |

**Nicht gewählt, aber als optionale Erweiterung empfohlen**: Grundsätzlich sinnvoll für
Disaster Recovery. Wird in der initialen Version jedoch nicht implementiert – stattdessen
erfolgen Offsite-Backups auf verschlüsselten USB-Datenträgern, die physisch an einem
zweiten Standort gelagert werden (einfacher, kein Internet nötig, keine AVV erforderlich).

### Alternative C: On-Premises (gewählt) ✅

| Aspekt | Bewertung |
|--------|-----------|
| **Datensouveränität** | ✅ Vollständige physische Kontrolle – Daten verlassen nie das Gebäude |
| **DSGVO-Konformität** | ✅ Kein Auftragsverarbeiter, keine Drittland-Problematik, keine AVV |
| **Kosten** | ✅ Einmalige Hardwarekosten, keine laufenden Gebühren |
| **Angriffsfläche** | ✅ System nicht über Internet erreichbar – nur LAN-Zugriff |
| **Unabhängigkeit** | ✅ Kein Internet für Betrieb erforderlich |
| **Verfügbarkeit** | ⚠️ Abhängig von lokaler Hardware (mitigiert durch Dienst-Neustart, USV) |
| **Zugriff von extern** | ❌ Kein Home-Office-Zugriff (akzeptabel – Arbeit findet im Büro statt) |
| **Backup** | ⚠️ Muss eigenverantwortlich organisiert werden (USB-Medien, Rotation) |
| **Hardware-Wartung** | ⚠️ Hardwaredefekt erfordert manuellen Austausch |

## Begründung

### 1. Maximaler Datenschutz durch physische Kontrolle

Die verarbeiteten Daten gehören zur **sensibelsten Kategorie**: Adressen und persönliche
Informationen von Frauen, die vor gewalttätigen Partnern Schutz suchen. Ein Datenleck
kann Menschenleben gefährden. Die einzig angemessene Schutzmaßnahme ist, diese Daten
**niemals** die physische Kontrolle des Vereins verlassen zu lassen.

On-Premises eliminiert folgende Risiken vollständig:
- Unbefugter Zugriff durch Cloud-Provider-Mitarbeiter
- Zugriff durch ausländische Behörden (US CLOUD Act, FISA 702)
- Datenverlust durch Cloud-Provider-Insolvenz oder Account-Sperrung
- Man-in-the-Middle-Angriffe auf der Internet-Strecke

### 2. DSGVO-Konformität ohne juristische Grauzone

| DSGVO-Artikel | Umsetzung durch On-Premises |
|---------------|----------------------------|
| Art. 5 (1c) – Datenminimierung | Keine unnötige Kopie bei Dritten |
| Art. 25 – Privacy by Design | Architektonisch kein Datenabfluss möglich |
| Art. 28 – Auftragsverarbeitung | Entfällt komplett – kein externer Verarbeiter |
| Art. 32 – Technische Maßnahmen | Physische Zugangskontrolle, Verschlüsselung lokal |
| Art. 44–49 – Drittlandtransfer | Ausgeschlossen – Daten bleiben in Deutschland |

Bei einer Cloud-Lösung müsste der Verein:
- Einen AVV mit dem Provider abschließen und regelmäßig prüfen
- Die Rechtmäßigkeit des Drittlandtransfers nachweisen (bei US-Anbietern problematisch)
- Bei Datenschutzverletzungen des Providers mithaften
- Regelmäßige Audits des Cloud-Providers durchführen

All dies entfällt bei On-Premises.

### 3. Kosteneffizienz für Vereinsbetrieb

| Modell | Einmalig | Laufend (pro Jahr) | 5-Jahres-TCO |
|--------|----------|-------------------|--------------|
| On-Premises (Mini-PC + USV) | ~800 € | ~50 € (Strom, USB-Medien) | ~1.050 € |
| Cloud (Hetzner CX31 + Backup) | 0 € | ~600 € | ~3.000 € |
| Cloud (Azure B2s + SQL Basic) | 0 € | ~960 € | ~4.800 € |

Für einen spendenfinanzierten Verein sind laufende Kosten problematischer als einmalige
Investitionen. On-Premises amortisiert sich bereits im zweiten Jahr.

### 4. Minimale Angriffsfläche

Das System ist **nicht über das Internet erreichbar**. Es existiert:
- Kein offener Port nach außen
- Keine öffentliche IP-Adresse
- Keine DNS-Einträge
- Kein SSL-Zertifikat für externe Domains

Ein Angreifer müsste physisch im LAN des Vereins sein, um das System zu erreichen.
Dies reduziert die Bedrohungslandschaft auf lokale Angriffe (Insider, physischer Einbruch),
die durch organisatorische Maßnahmen (Zutrittskontrolle, Bildschirmsperre, Passwortpolicy)
adressiert werden.

### 5. Keine Internet-Abhängigkeit

Der Verein ist nicht von einem ISP oder Cloud-Provider abhängig. Das System funktioniert
auch bei:
- Internet-Ausfall
- Provider-Insolvenz
- Vertragsstreitigkeiten mit Cloud-Anbietern
- DDoS-Angriffen auf die Internet-Leitung

## Risiken und Mitigationen

| Risiko | Wahrscheinlichkeit | Auswirkung | Mitigation |
|--------|-------------------|------------|------------|
| Hardware-Defekt | Mittel | Hoch | USV gegen Stromausfall, RAID oder regelmäßige Backups, Ersatzhardware-Plan |
| Feuer / Wasserschaden | Niedrig | Sehr hoch | Verschlüsselte Offsite-Backups auf USB (zweiter Standort) |
| Diebstahl des Servers | Niedrig | Hoch | Festplattenverschlüsselung (BitLocker), Server in abschließbarem Raum |
| Kein automatisches Patching | Mittel | Mittel | Quartalsweise manuelle Windows-Updates, System nicht im Internet |
| Kein externer Zugriff | – | Niedrig | Akzeptiert: Arbeit findet ausschließlich im Büro statt |

## Konsequenzen

### Positiv
- Höchstmöglicher Datenschutz – Daten verlassen nie die physischen Räume des Vereins
- Vollständige DSGVO-Konformität ohne juristische Grauzone oder Drittanbieter-Abhängigkeit
- Keine laufenden Kosten für Hosting oder Cloud-Dienste
- Minimale Angriffsfläche durch fehlende Internet-Exposition
- Vollständige Unabhängigkeit von externen Dienstleistern und Internet-Verfügbarkeit

### Negativ
- Kein Remote-Zugriff möglich (kein Home-Office) – akzeptabel für den Anwendungsfall
- Hardware-Wartung liegt in Vereinsverantwortung – mitigiert durch einfache Hardware (Mini-PC)
- Backup-Disziplin muss organisatorisch sichergestellt werden (USB-Rotation)
- Kein automatisches Failover bei Hardware-Ausfall – akzeptabel bei Bürozeiten-Betrieb

### Neutral
- Erfordert einmalige Hardware-Beschaffung (Windows-Server-fähiger Mini-PC, USV, USB-Medien)
- Windows-Updates müssen manuell eingespielt werden (kein Internet = kein Auto-Update)
- Backup-Strategie muss in separatem Dokument definiert werden
