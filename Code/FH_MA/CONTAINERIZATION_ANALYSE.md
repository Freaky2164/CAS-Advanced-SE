# 🐳 Containerization-Analyse: Frauenhaus-Verwaltung

## Die Frage: "Lohnt sich Docker/Container für dieses Projekt?"

### Kurze Antwort: **JA, aber nur für die NEUE Architektur (SOLL).**

Für die **alte Swing-App (IST)**: Nein, bringt nichts.  
Für die **neue Web-App (SOLL)**: Absolut, und zwar massiv!

---

## 1. Warum Container für die ALTE App NICHT sinnvoll sind

Die aktuelle App ist ein **Java Swing Fat Client** mit GUI:

```
PROBLEM: Swing braucht einen Bildschirm (Display)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Container = "Headless" (kein Monitor, keine GUI)
Swing = "Headed" (braucht Monitor, Tastatur, Maus)

→ Swing-App in Docker = unsinnig
→ Man kann es mit X11-Forwarding hacken, aber das
  ist KOMPLIZIERTER als ohne Docker!

EINZIGE Ausnahme: SQL Server in Container
→ Das wäre möglich, aber Overkill für 1 Datenbank
```

---

## 2. Warum Container für die NEUE App IDEAL sind

Die neue Architektur (Spring Boot + Angular + SQL Server) ist **perfekt für Container**:

```
┌──────────────────────────────────────────────────────┐
│              DOCKER COMPOSE STACK                     │
│                                                       │
│  ┌─────────────────────┐  ┌───────────────────────┐  │
│  │  frauenhaus-app     │  │  frauenhaus-db        │  │
│  │  ────────────────   │  │  ──────────────────   │  │
│  │  Spring Boot 3.x    │  │  SQL Server 2022      │  │
│  │  + Angular SPA      │  │  (mcr.microsoft.com/  │  │
│  │  (alles in einem)   │  │   mssql/server:2022)  │  │
│  │                     │  │                       │  │
│  │  Port: 8443 (HTTPS) │  │  Port: 1433 (intern)  │  │
│  │  Java 17 + nginx    │  │  Volume: db-data      │  │
│  └──────────┬──────────┘  └───────────┬───────────┘  │
│             │                         │               │
│             └────────── JDBC ─────────┘               │
│                                                       │
│  ┌─────────────────────┐                              │
│  │  frauenhaus-backup  │                              │
│  │  ────────────────   │                              │
│  │  Automatisches      │                              │
│  │  SQL Backup         │                              │
│  │  (Cron-Job)         │                              │
│  │  Volume: backups    │                              │
│  └─────────────────────┘                              │
│                                                       │
│  Volumes:                                             │
│  ├─ db-data:    Datenbank-Dateien (persistent)       │
│  ├─ backups:    Backup-Dateien (persistent)          │
│  └─ templates:  Word/Excel-Vorlagen (persistent)     │
│                                                       │
└──────────────────────────────────────────────────────┘
```

---

## 3. Vergleich: Mit vs. Ohne Container

### **Ohne Container (klassisch)**

```
INSTALLATION auf Windows Server:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Schritt 1: Java 17 JDK installieren           → 15 Min
Schritt 2: Umgebungsvariablen setzen          → 10 Min
Schritt 3: SQL Server installieren            → 30 Min
Schritt 4: SQL Server konfigurieren           → 20 Min
Schritt 5: Datenbank erstellen                → 10 Min
Schritt 6: Flyway-Migrationen ausführen       → 5 Min
Schritt 7: Spring Boot JAR deployen           → 5 Min
Schritt 8: Windows-Service einrichten (WinSW) → 15 Min
Schritt 9: HTTPS/SSL konfigurieren           → 20 Min
Schritt 10: Firewall-Regeln setzen           → 10 Min
Schritt 11: Backup-Jobs einrichten           → 20 Min
Schritt 12: Testen                           → 15 Min
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:                                         ~3 STUNDEN

Update:
  - Java updaten? → Kompatibilitätsprobleme?
  - SQL Server patchen? → Downtime nötig!
  - Spring Boot JAR tauschen → Service stoppen/starten
  → Pro Update: 30-60 Minuten

Umzug auf neuen Server:
  - Alles nochmal von vorne!
  → 3+ Stunden
```

### **Mit Docker Compose**

```
INSTALLATION auf Windows Server:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Schritt 1: Docker Desktop installieren        → 10 Min
Schritt 2: docker-compose.yml hinkopieren     → 1 Min
Schritt 3: docker compose up -d              → 5 Min (Downloads)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:                                         ~16 MINUTEN!

Update:
  docker compose pull        ← Neue Images laden
  docker compose up -d       ← Neu starten
  → Pro Update: 2-5 Minuten!

Umzug auf neuen Server:
  1. Docker installieren
  2. docker-compose.yml + Volumes kopieren
  3. docker compose up -d
  → 20 Minuten!
```

### **Vergleichstabelle**

| Aspekt | Ohne Docker | Mit Docker |
|--------|------------|------------|
| **Erstinstallation** | ~3 Stunden | ~16 Minuten |
| **Update** | 30-60 Min | 2-5 Min |
| **Serverumzug** | 3+ Stunden | 20 Min |
| **Backup** | Manuell konfigurieren | Im Compose integriert |
| **Rollback** | JAR-Datei austauschen | `docker compose up -d --rollback` |
| **"Works on my machine"** | Häufig | Nie (Container = identisch) |
| **Komplexität für Admin** | Hoch (12 Schritte) | Niedrig (3 Schritte) |
| **Lernkurve** | Java, SQL, Windows Services | `docker compose up` |
| **Voraussetzung** | Java, SQL Server, WinSW | Nur Docker Desktop |

---

## 4. Konkrete Umsetzung: docker-compose.yml

So würde das in der Praxis aussehen:

```yaml
# docker-compose.yml
# Frauenhaus Adress- und Bußgeldverwaltung
# ==========================================
# Start:   docker compose up -d
# Stop:    docker compose down
# Logs:    docker compose logs -f
# Update:  docker compose pull && docker compose up -d
# Backup:  Automatisch (alle 24h)

services:

  # ===== WEB-ANWENDUNG (Spring Boot + Angular) =====
  app:
    image: frauenhaus/verwaltung:latest
    # Oder lokal gebaut:
    # build: ./frauenhaus-backend
    container_name: frauenhaus-app
    restart: always              # ← Auto-Neustart bei Absturz!
    ports:
      - "443:8443"               # HTTPS nach außen
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:sqlserver://db:1433;databaseName=frauenhaus;encrypt=true;trustServerCertificate=true
      - SPRING_DATASOURCE_USERNAME=sa
      - SPRING_DATASOURCE_PASSWORD_FILE=/run/secrets/db_password
      - SPRING_FLYWAY_ENABLED=true
      - SERVER_SSL_KEY_STORE=/certs/keystore.p12
      - SERVER_SSL_KEY_STORE_PASSWORD_FILE=/run/secrets/ssl_password
    volumes:
      - templates:/app/vorlagen   # Word/Excel-Vorlagen
      - reports:/app/reports      # Generierte Reports
      - ./certs:/certs:ro         # SSL-Zertifikate
    secrets:
      - db_password
      - ssl_password
    depends_on:
      db:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "-k", "https://localhost:8443/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # ===== DATENBANK (SQL Server 2022) =====
  db:
    image: mcr.microsoft.com/mssql/server:2022-latest
    container_name: frauenhaus-db
    restart: always
    environment:
      - ACCEPT_EULA=Y
      - MSSQL_SA_PASSWORD_FILE=/run/secrets/db_password
      - MSSQL_PID=Express        # Express Edition (kostenlos)
      # Oder: MSSQL_PID=Developer (mehr Features, auch kostenlos)
    ports:
      - "127.0.0.1:1433:1433"    # NUR lokal erreichbar!
    volumes:
      - db-data:/var/opt/mssql    # Datenbank-Dateien (PERSISTENT!)
    secrets:
      - db_password
    healthcheck:
      test: /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$$(cat /run/secrets/db_password)" -Q "SELECT 1" -C -b
      interval: 15s
      timeout: 10s
      retries: 5
      start_period: 30s

  # ===== AUTOMATISCHES BACKUP =====
  backup:
    image: mcr.microsoft.com/mssql-tools:latest
    container_name: frauenhaus-backup
    restart: always
    entrypoint: /bin/bash
    command: >
      -c "
      while true; do
        echo \"[BACKUP] $$(date): Starte Full-Backup...\"
        /opt/mssql-tools18/bin/sqlcmd -S db -U sa -P \"$$(cat /run/secrets/db_password)\" -C -Q \"
          BACKUP DATABASE [frauenhaus]
          TO DISK = N'/backups/frauenhaus_$$(date +%Y%m%d_%H%M%S).bak'
          WITH COMPRESSION, CHECKSUM, STATS = 10
        \"
        echo \"[BACKUP] $$(date): Backup abgeschlossen.\"

        echo \"[CLEANUP] Lösche Backups älter als 30 Tage...\"
        find /backups -name '*.bak' -mtime +30 -delete

        echo \"[BACKUP] Nächstes Backup in 24 Stunden.\"
        sleep 86400
      done
      "
    volumes:
      - backups:/backups          # Backup-Dateien (PERSISTENT!)
    secrets:
      - db_password
    depends_on:
      db:
        condition: service_healthy

# ===== PERSISTENTE DATEN =====
volumes:
  db-data:        # Datenbank (NIEMALS löschen!)
    name: frauenhaus-db-data
  backups:        # Backup-Dateien
    name: frauenhaus-backups
  templates:      # Word/Excel-Vorlagen
    name: frauenhaus-templates
  reports:        # Generierte Reports
    name: frauenhaus-reports

# ===== PASSWÖRTER (nicht im Klartext!) =====
secrets:
  db_password:
    file: ./secrets/db_password.txt
  ssl_password:
    file: ./secrets/ssl_password.txt
```

---

## 5. Für den Nicht-Techniker: Wie bedient man das?

### **Die 5 Docker-Befehle, die man braucht:**

```
┌─────────────────────────────────────────────────────┐
│  DOCKER FÜR ANFÄNGER – Die 5 Befehle               │
├─────────────────────────────────────────────────────┤
│                                                     │
│  1. STARTEN:                                        │
│     docker compose up -d                            │
│     (Startet alles: App + DB + Backup)              │
│                                                     │
│  2. STOPPEN:                                        │
│     docker compose down                             │
│     (Stoppt alles. Daten bleiben erhalten!)         │
│                                                     │
│  3. STATUS:                                         │
│     docker compose ps                               │
│     (Zeigt: Läuft alles? Grün = OK)                │
│                                                     │
│  4. LOGS ANSCHAUEN:                                 │
│     docker compose logs -f                          │
│     (Zeigt was passiert, Strg+C zum Beenden)        │
│                                                     │
│  5. UPDATE:                                         │
│     docker compose pull                             │
│     docker compose up -d                            │
│     (Lädt neue Version + startet neu)               │
│                                                     │
│  FERTIG! Mehr braucht man nicht! 🎉                 │
└─────────────────────────────────────────────────────┘
```

### **Noch einfacher: Wrapper-Skripte**

Für den Admin, der nicht mal Docker-Befehle tippen will:

```batch
:: frauenhaus_starten.bat
@echo off
echo ===================================
echo Frauenhaus-Verwaltung wird gestartet
echo ===================================
cd /d "%~dp0"
docker compose up -d
echo.
echo [OK] App laeuft unter: https://localhost
echo Oeffne Chrome und gehe zu: https://localhost
pause
```

```batch
:: frauenhaus_stoppen.bat
@echo off
echo ===================================
echo Frauenhaus-Verwaltung wird gestoppt
echo ===================================
cd /d "%~dp0"
docker compose down
echo [OK] Anwendung gestoppt. Daten bleiben erhalten.
pause
```

```batch
:: frauenhaus_update.bat
@echo off
echo ===================================
echo Frauenhaus-Verwaltung UPDATE
echo ===================================
echo.
echo Lade neue Version herunter...
cd /d "%~dp0"
docker compose pull
echo.
echo Starte neue Version...
docker compose up -d
echo.
echo [OK] Update abgeschlossen!
echo App erreichbar unter: https://localhost
pause
```

```batch
:: frauenhaus_status.bat
@echo off
echo ===================================
echo Frauenhaus-Verwaltung STATUS
echo ===================================
cd /d "%~dp0"
docker compose ps
echo.
echo Wenn alle "running" zeigen = Alles OK
echo Wenn etwas "exited" zeigt = Problem!
echo.
pause
```

### **Der Alltag für den Admin:**

```
Morgens: Doppelklick auf frauenhaus_starten.bat
         (Falls es nicht schon läuft – Docker startet automatisch)
         → Fertig!

Bei Update: Doppelklick auf frauenhaus_update.bat
            → 2 Minuten warten
            → Fertig!

Bei Problem: Doppelklick auf frauenhaus_status.bat
             → Lesen was es sagt
             → Falls "exited": frauenhaus_starten.bat
             → Fertig!

Backup: Läuft automatisch im Container!
        → Admin muss NICHTS tun!
```

---

## 6. Vergleich aller Deployment-Optionen

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    DEPLOYMENT-OPTIONEN IM VERGLEICH                      │
├──────────────┬──────────────────┬──────────────────┬────────────────────┤
│ Kriterium    │ Bare Metal       │ Docker Compose   │ Cloud/SaaS         │
│              │ (Windows Service)│ (Container)      │ (Azure/AWS)        │
├──────────────┼──────────────────┼──────────────────┼────────────────────┤
│ Installation │ 3 Stunden        │ 16 Minuten       │ 0 (gehostet)       │
│ Update       │ 30-60 Min        │ 2-5 Min          │ Automatisch        │
│ Serverumzug  │ 3+ Stunden       │ 20 Min           │ Entfällt           │
│ Backup       │ Manuell konfig.  │ Im Compose       │ Automatisch        │
│ Rollback     │ JAR tauschen     │ Image-Tag ändern │ 1-Click            │
│ Kosten/Jahr  │ ~1,840€ + Server │ ~1,840€ + Server │ 6,000-24,000€     │
│ Kontrolle    │ Voll             │ Voll             │ Eingeschränkt      │
│ Admin-Skill  │ Mittel-Hoch      │ Niedrig          │ Sehr niedrig       │
│ Datenschutz  │ Lokal (✓)        │ Lokal (✓)        │ Cloud (?)          │
│ Offline      │ Ja               │ Ja               │ Nein               │
│ Skalierung   │ Schwer           │ Leicht           │ Sehr leicht        │
│ Vendor Lock  │ Nein             │ Nein             │ Ja                 │
├──────────────┼──────────────────┼──────────────────┼────────────────────┤
│ EMPFEHLUNG   │ ⚠️ Nur für IT    │ ✅ BESTE WAHL    │ ⚠️ Teuer + DSGVO  │
│ für diesen   │ Profis           │ für dieses       │ Prüfung nötig      │
│ Use Case     │                  │ Projekt!         │                    │
└──────────────┴──────────────────┴──────────────────┴────────────────────┘
```

### **Warum Docker Compose die BESTE Wahl ist:**

```
✅ Lokale Daten (DSGVO-konform, sensible Personendaten!)
✅ Offline-fähig (Frauenhaus braucht keine Internet-Abhängigkeit)
✅ Günstig (Docker Desktop ist kostenlos für kleine Organisationen)
✅ Einfach (5 Befehle reichen, oder 4 Batch-Dateien)
✅ Automatisches Backup (im Compose integriert)
✅ Reproduzierbar (neuer Server = identisches Setup)
✅ Rollback (einfach altes Image-Tag verwenden)
✅ Kein Vendor Lock-in (Standard-Technologie)
```

### **Warum NICHT Cloud/SaaS:**

```
❌ Sensible Personendaten (Frauenhaus = Opfer häuslicher Gewalt!)
   → DSGVO Art. 9: Besondere Kategorien personenbezogener Daten
   → Cloud-Hosting erfordert aufwändige Datenschutzfolgenabschätzung
   → Lokale Speicherung ist SICHERER für diesen Use Case

❌ Internet-Abhängigkeit
   → Was wenn Internet ausfällt?
   → Frauenhaus muss IMMER arbeiten können

❌ Kosten
   → 6,000-24,000€/Jahr vs. 1,840€/Jahr (Docker lokal)
   → Für ein kleines Frauenhaus zu teuer
```

---

## 7. Komplettes Docker-Setup: Was der Admin bekommt

### **Verzeichnisstruktur auf dem Server:**

```
C:\frauenhaus\
├── docker-compose.yml        ← Die Hauptkonfiguration
├── secrets/
│   ├── db_password.txt       ← Datenbank-Passwort
│   └── ssl_password.txt      ← SSL-Zertifikat-Passwort
├── certs/
│   └── keystore.p12          ← HTTPS-Zertifikat
├── frauenhaus_starten.bat    ← Doppelklick = Start
├── frauenhaus_stoppen.bat    ← Doppelklick = Stop
├── frauenhaus_update.bat     ← Doppelklick = Update
├── frauenhaus_status.bat     ← Doppelklick = Status
├── frauenhaus_backup_jetzt.bat ← Sofort-Backup
└── README.txt                ← Kurzanleitung (ausdruckbar)
```

### **Die Admin-Anleitung (auf 1 Seite, ausdruckbar):**

```
╔════════════════════════════════════════════════════════╗
║  FRAUENHAUS VERWALTUNG – ADMIN-KURZANLEITUNG          ║
║  (Diese Seite ausdrucken & an den Monitor kleben!)    ║
╠════════════════════════════════════════════════════════╣
║                                                        ║
║  APP STARTEN:                                          ║
║    → Doppelklick: frauenhaus_starten.bat               ║
║    → Warte 30 Sekunden                                 ║
║    → Browser: https://localhost                        ║
║                                                        ║
║  APP STOPPEN:                                          ║
║    → Doppelklick: frauenhaus_stoppen.bat               ║
║    → Daten bleiben erhalten!                           ║
║                                                        ║
║  UPDATE:                                               ║
║    → Doppelklick: frauenhaus_update.bat                ║
║    → Warte 2 Minuten                                   ║
║    → Browser aktualisieren (F5)                        ║
║                                                        ║
║  STATUS PRÜFEN:                                        ║
║    → Doppelklick: frauenhaus_status.bat                ║
║    → "running" = OK  ✓                                 ║
║    → "exited" = Problem! → starten.bat ausführen       ║
║                                                        ║
║  BACKUP:                                               ║
║    → Automatisch (alle 24h)!                           ║
║    → Manuell: frauenhaus_backup_jetzt.bat              ║
║    → Backups in: C:\frauenhaus\backups\                ║
║                                                        ║
║  NOTFALL:                                              ║
║    1. frauenhaus_stoppen.bat                           ║
║    2. Warte 10 Sekunden                                ║
║    3. frauenhaus_starten.bat                           ║
║    4. Falls immer noch kaputt: IT anrufen!             ║
║                                                        ║
║  IT-SUPPORT: Tel. _______________                      ║
║                                                        ║
╚════════════════════════════════════════════════════════╝
```

---

## 8. Docker: Vorteile für Backup & Recovery

### **Backup mit Docker = kinderleicht**

```
AUTOMATISCH (im docker-compose.yml integriert):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ Backup-Container läuft parallel
✓ Jeden Tag um 23:00: Full Backup
✓ Alte Backups (>30 Tage) automatisch gelöscht
✓ Admin muss NICHTS tun!

MANUELL (Sofort-Backup):
━━━━━━━━━━━━━━━━━━━━━━
Doppelklick: frauenhaus_backup_jetzt.bat
→ Backup wird sofort erstellt
→ Gespeichert in: C:\frauenhaus\backups\

RECOVERY:
━━━━━━━━
1. frauenhaus_stoppen.bat
2. Backup-Datei auswählen (z.B. frauenhaus_20240626.bak)
3. frauenhaus_restore.bat frauenhaus_20240626.bak
4. frauenhaus_starten.bat
→ Fertig! Daten wiederhergestellt!
```

### **Komplettes System-Backup (Docker Volumes):**

```bash
# Alles sichern (DB + App-Daten + Backups)
docker compose down
xcopy C:\frauenhaus E:\backup\frauenhaus\ /E /I

# Wiederherstellen auf neuem Server:
xcopy E:\backup\frauenhaus C:\frauenhaus\ /E /I
docker compose up -d
# → FERTIG! Komplettes System wiederhergestellt!
```

---

## 9. Sicherheit mit Docker

### **Vorteile:**

```
✅ Container-Isolation
   → App kann NUR auf DB zugreifen (internes Netzwerk)
   → DB ist von außen NICHT erreichbar (nur localhost:1433)
   → Angreifer von außen sehen nur Port 443 (HTTPS)

✅ Secrets Management
   → Passwörter in separaten Dateien (nicht im docker-compose.yml)
   → Secrets werden als Dateien gemountet (nicht als Env-Variablen)
   → Sicherer als .ini-Datei mit Klartext-Passwort!

✅ Immutable Infrastructure
   → Container-Image = unveränderlich
   → Kein "jemand hat mal was auf dem Server geändert"
   → Reproduzierbar und auditierbar

✅ Automatische Updates
   → docker compose pull → Neue Sicherheitspatches
   → Kein manuelles Java/SQL-Server Patching
```

### **Gegenüber IST-Zustand:**

| Sicherheitsaspekt | IST (Alt) | SOLL (Docker) |
|---|---|---|
| Passwörter | Klartext in .ini | Docker Secrets (verschlüsselt) |
| Netzwerk | DB offen auf Port 1433 | DB nur intern erreichbar |
| Isolation | App + DB auf gleicher Ebene | Container-Isolation |
| Updates | Manuell, oft vergessen | `docker compose pull` |
| Audit | Kein Log | Container-Logs + Audit-Trail |

---

## 10. Fazit & Empfehlung

### **Die Empfehlung für dieses Projekt:**

```
╔══════════════════════════════════════════════════════════╗
║                                                          ║
║  EMPFEHLUNG: Docker Compose auf Windows Server           ║
║                                                          ║
║  Gründe:                                                 ║
║  ✅ Installation: 16 Min statt 3 Stunden                ║
║  ✅ Update: 2 Min statt 1 Stunde                        ║
║  ✅ Backup: Automatisch, kein Admin-Aufwand              ║
║  ✅ Sicherheit: Isolation, Secrets, nur HTTPS            ║
║  ✅ Admin-freundlich: 4 Batch-Dateien reichen            ║
║  ✅ Daten lokal: DSGVO-konform, kein Cloud-Risiko        ║
║  ✅ Reproduzierbar: Neuer Server in 20 Minuten           ║
║  ✅ Kosten: Docker Desktop kostenlos für < 250 MA        ║
║                                                          ║
║  Voraussetzung:                                          ║
║  - Windows 10/11 Pro oder Windows Server 2019+           ║
║  - Docker Desktop installieren (einmalig)                ║
║  - 4 GB RAM minimum, 8 GB empfohlen                      ║
║                                                          ║
║  Risiken:                                                ║
║  - Docker Desktop Update kann Probleme machen            ║
║    → Lösung: Version pinnen, nur manuell updaten         ║
║  - Linux-Container auf Windows = WSL2 nötig              ║
║    → Automatisch von Docker Desktop installiert          ║
║                                                          ║
╚══════════════════════════════════════════════════════════╝
```

### **Migrationsplan mit Docker:**

```
Phase 1 (Woche 1-2): Docker-Setup
  ✓ Docker Desktop auf Server installieren
  ✓ docker-compose.yml erstellen
  ✓ SQL Server Container mit Daten-Migration
  ✓ Testen

Phase 2 (Woche 3-6): Spring Boot App containerisieren
  ✓ Dockerfile für Backend erstellen
  ✓ Angular Frontend einbinden
  ✓ Health-Checks konfigurieren
  ✓ HTTPS einrichten

Phase 3 (Woche 7-8): Backup & Monitoring
  ✓ Backup-Container einrichten
  ✓ Log-Aggregation
  ✓ Admin-Batch-Dateien erstellen
  ✓ Admin-Schulung (30 Min)

Phase 4 (Woche 9-10): Go-Live
  ✓ Parallelbetrieb (alte + neue App)
  ✓ Datenmigration
  ✓ Benutzer-Schulung
  ✓ Alte App abschalten
```
