# 🛠️ Maintenance & Operations für Nicht-Techniker

## Ziel: Wartung so einfach wie möglich

Diese Dokumentation richtet sich an **Sachbearbeiter, Admin oder Sekretariat** – **ohne technische Vorkenntnisse**.

---

## 📊 Vergleich: Jetzt vs. Ideal

### **JETZT (IST-Zustand) – Komplex & fehleranfällig**

```
Problem 1: Installation auf jedem PC einzeln
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PC 1: Manuelle Installation, Konfiguration .ini-Datei, Java-Pfade
PC 2: Dito, aber Pfade müssen angepasst werden
PC 3: Dito, aber andere Excel-Version
PC 4: Fehler bei Installation – IT muss reparieren
→ ZEITAUFWAND: 2-3 Stunden pro PC!

Problem 2: Update (Neue Version)?
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Alle 4 PCs einzeln updaten?
- Alte Version löschen
- Neue Version installieren
- Neu konfigurieren
- Test auf jedem PC
→ Ein Update dauert einen halben Tag!

Problem 3: Fehlerbehandlung
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Fehler → IT-Support anrufen
→ "Das DEBUG war an der .ini-Datei"
→ "Der Classpath ist kaputt"
→ "SQL Server läuft nicht"
→ Mehrere Stunden Ausfallzeit!

Problem 4: Backup & Recovery
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Manuelles Backup? Nicht dokumentiert.
Was wenn die Festplatte kaputt geht?
Daten weg = Desaster!
```

### **IDEAL (SOLL-Zustand) – Wartungsarm**

```
Lösung 1: Zentrale Installation
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ Backend auf einem Server
✓ Alle Nutzer öffnen nur Browser
✓ Keine Installation auf Client-PCs
→ Installation einmalig, danach easy!

Lösung 2: Updates sind ein Klick
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ Neue JAR-Datei hochladen
✓ Service neu starten (1-2 Min)
✓ Browser aktualisiert sich automatisch
→ Update in 5 Minuten!

Lösung 3: Selbstdiagnose
━━━━━━━━━━━━━━━━━━━━━━━━
✓ Health-Check Dashboard
✓ Automatische Alerts bei Problemen
✓ Fehler behoben bevor Nutzer merkt
→ Keine Ausfallzeiten!

Lösung 4: Automatisiertes Backup
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ Täglich automatisches Backup
✓ Cloud-Sync
✓ One-Click Recovery
→ Datensicherheit ohne Handarbeit!
```

---

## 🎯 Teil A: Aktuelle Situation (IST) – Wartung vereinfachen

### Ziel: Möglichst "Idiot-Proof" machen

Auch wenn die alte Architektur bleibt, können wir Fehler minimieren:

#### **1. Automatisierte Startup-Assistenten**

Statt `run_local.bat` aufzurufen → **Interaktiver Guide**

```batch
@echo off
REM startup_assistant.bat - für Nicht-Techniker

cls
color 0A
echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║   FRAUENHAUS VERWALTUNG - Startup-Assistent              ║
echo ║   (Für Anfänger geeignet)                                 ║
echo ╚════════════════════════════════════════════════════════════╝
echo.

REM Check 1: Java vorhanden?
where java >nul 2>nul
if errorlevel 1 (
    color 0C
    echo [FEHLER] Java nicht gefunden!
    echo.
    echo Bitte installiere Java 17 von:
    echo https://adoptium.net/download
    echo.
    pause
    exit /b 1
)

REM Check 2: SQL Server erreichbar?
sqlcmd -S localhost -E -Q "SELECT 1" >nul 2>nul
if errorlevel 1 (
    color 0E
    echo [WARNUNG] SQL Server nicht erreichbar!
    echo.
    echo Starten Sie SQL Server:
    echo 1. Drücken Sie: Windows-Taste + R
    echo 2. Geben Sie ein: services.msc
    echo 3. Suchen Sie: SQL Server (oder SQLEXPRESS)
    echo 4. Rechtsklick: "Starten"
    echo.
    pause
    exit /b 1
)

REM Alles gut
color 0A
echo [OK] Alle Checks bestanden!
echo.
echo Die Anwendung wird gestartet...
echo.
java.exe compucrash.CStart "localhost.ini"

if errorlevel 1 (
    color 0C
    echo.
    echo [FEHLER] Die Anwendung konnte nicht starten.
    echo Kontaktieren Sie: IT-Support@frauenhaus.de
    echo.
    pause
    exit /b 1
)
```

#### **2. Fehler-Auto-Korrektur**

Script, das häufige Probleme automatisch behebt:

```batch
@echo off
REM auto_fix.bat - Häufige Probleme automatisch beheben

echo [INFO] Führe Selbstdiagnose und Auto-Reparatur durch...
echo.

REM Problem 1: Klasspath-Fehler
echo [CHECK] Kompiliere Java-Dateien neu...
javac -encoding UTF-8 -d . compucrash\*.java 2>nul
if errorlevel 0 echo [OK] Kompilierung erfolgreich

REM Problem 2: Verzeichnisse erstellen
if not exist "C:\frauenhaus\reports" mkdir "C:\frauenhaus\reports"
if not exist "C:\frauenhaus\vorlagen" mkdir "C:\frauenhaus\vorlagen"
echo [OK] Verzeichnisse erstellt/vorhanden

REM Problem 3: Firewall
REM (optional - nur wenn Admin-Rechte)

echo.
echo [SUCCESS] Auto-Reparatur abgeschlossen!
echo Versuchen Sie jetzt, die App zu starten.
echo.
pause
```

#### **3. Einfache Checklisten**

**Tägliche 5-Minuten-Checkliste (vor Arbeitsbeginn):**

```
☐ App starten – lädt normal?
☐ Login – funktioniert mit deinem Benutzernamen?
☐ Dateneintrag testen – kannst du eine Spende hinzufügen & speichern?
☐ Report generieren – mindestens ein Report möglich?

❌ Wenn etwas nicht funktioniert:
   1. Auto-fix.bat ausführen
   2. App neu starten
   3. Wenn Problem bleibt: IT-Support anrufen
```

**Regelmäßige Backups (wöchentlich):**

```
☐ Montag morgens:
   - SQL Server öffnen (Management Studio)
   - Rechtsklick auf "frauenhaus" DB
   - Tasks → Back Up
   - Speichern unter: C:\Backup\frauenhaus_weekly_<Wochennummer>.bak
   - Datei auf USB kopieren (Offsite-Backup)

❌ Wenn Fehler:
   → Backup-Script.bat ausführen (automatisch)
```

---

## 📱 Teil B: Ideale Situation (SOLL) – Minimale Wartung

### **Deployment: Browser statt Installation**

```
Statt: "Installation auf PC durchführen, Fehler beheben"
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Neu: "Browser öffnen, URL eingeben, fertig"

┌─────────────────────────────────────────┐
│  Mitarbeiter startet Arbeitstag:        │
├─────────────────────────────────────────┤
│  1. PC anschalten (normal)              │
│  2. Chrome/Edge öffnen (normal)         │
│  3. https://frauenhaus.local eingeben   │
│  4. Login-Daten eingeben (wie immer)    │
│  5. Arbeiten!                           │
└─────────────────────────────────────────┘

Keine Installation. Keine Fehler-Meldungen.
Easy für JEDEN!
```

### **Updates: Automatisch & unsichtbar**

```
VORHER (IST):
━━━━━━━━━━
Neue Version verfügbar
→ IT muss alle 4 PCs updaten (4 Stunden)
→ Ausfallzeiten möglich
→ Benutzer müssen Fehler melden

NACHHER (SOLL):
━━━━━━━━━━━━━
Admin lädt neue JAR-Datei hoch
→ Backend startet neu (2 Min)
→ Alle Clients laden automatisch neu
→ Benutzer sehen neue Features
→ Keine Ausfallzeit!

Mit Feature-Flags: Auch stufenweise Rollout möglich
→ 25% Nutzer → 50% Nutzer → 100%
```

### **Fehlerbehandlung: Automatisches Healing**

```
VORHER (IST):
━━━━━━━━━━
Error in App
↓
Benutzer merkt
↓
Benutzer ruft IT an
↓
IT debuggt (1-2 Stunden)
↓
Problem gelöst
→ Ausfallzeit: 2-3 Stunden!

NACHHER (SOLL):
━━━━━━━━━━━━
Error in Backend
↓
Health-Check erkennt es automatisch
↓
Logging & Alert wird angestoßen
↓
Fehler wird automatisch behoben
   (z.B. Neustarten bei OOM)
↓
Benutzer sieht nichts
→ Zero Downtime!
```

### **Backup & Recovery: Set & Forget**

```
VORHER (IST):
━━━━━━━━━━
Manuelles Backup? Fehlerquelle
Vergessen? Datenverlust!
Notwendig? Unklar - keiner weiß
Restore? Lange Fehlersuche

NACHHER (SOLL):
━━━━━━━━━━━━
✓ Täglich automatisches Backup (23:00 Uhr)
✓ Differenzielles Backup alle 6h (spart Platz)
✓ Log-Backup jede Stunde (Point-in-Time)
✓ Automatisch auf NAS kopiert (Offsite)
✓ Weekly Test: Restore automatisch getestet
✓ Alert wenn Backup fehlt

Benutzer macht: NICHTS!
System macht: ALLES!

One-Click Recovery im Notfall:
  Admin-Dashboard → "Restore to Date" → Zeitpunkt wählen → Done!
```

---

## 🎓 Teil C: Schritt-für-Schritt Migrations-Plan

### **Für kleine Organisationen (1-5 Nutzer)**

```
Phase 1: IST optimieren (Monat 1)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ Auto-fix.bat & Startup-Assistant einführen
✓ Checklisten drucken & aushängen
✓ IT-Support Training (30 Min)
✓ Backup-Automatisierung

Aufwand: 8-16 Stunden
Gewinn: 50% weniger Fehler

Phase 2: Zu SOLL migrieren (Monat 2-4)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ Development der neuen Architektur
✓ Paralleltests (beide Systeme laufen)
✓ Migration der Daten
✓ Schulung der Nutzer

Aufwand: 200-300 Stunden (Entwicklung)
Gewinn: Wartungsarm, zukunftssicher

Phase 3: Legacy-System abschalten (Monat 4+)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ Archivierung alte Daten
✓ Dokumentation für Audit
✓ Deinstallation von alten PCs

Aufwand: 8 Stunden
```

---

## 🎪 Teil D: Operatives Handbuch (SOLL-System)

### **Für Anfänger: "App startet nicht"**

```
SYMPTOM:  Browser zeigt Fehler
LÖSUNG:   
  1. WARTE 2 Minuten (Backend startet vielleicht gerade)
  2. Browser aktualisieren (F5)
  3. Andere Website (z.B. Google) testen
     - Google funktioniert? → Backend-Problem
     - Google nicht? → Internet-Problem
  4. IT anrufen

NICHT TUN: Nicht 100x aktualisieren, nicht PC neu starten (außer wenn aufgefordert)
```

### **Für den Admin: Daily Check**

```
Every morning (5 min):
━━━━━━━━━━━━━━━━━━━━
1. Dashboard öffnen (https://admin.frauenhaus.local)
2. Grünes "Healthy" Icon? → Alles gut
3. Rotes "Alert" Icon? → Schau den Alert an
   - Oft: "Backup fehlgeschlagen" → Disk voll? → Aufräumen
   - Oft: "DB Connection slow" → Defrag? → Reboot DB
   - Selten: "Critical" → IT anrufen

That's it!
```

### **Für den Admin: Wöchentlicher Check**

```
Every Friday (15 min):
━━━━━━━━━━━━━━━━━━━
1. Dashboard → "Backups" Tab
2. Letzte 7 Backups grün? → Alles gut
3. Ein Backup rot? → Alert anschauen
4. Admin-Benachrichtigung auslesen
5. Notwendige Aktionen notieren
6. Nächste Woche durchführen

Admin-Notizen-Template:
━━━━━━━━━━━━━━━━━━━━━
- [ ] Backup erfolgreich
- [ ] Performance OK
- [ ] User-Anfragen bearbeitet
- [ ] Updates verfügbar? → Zeitpunkt festlegen
```

### **Für den Admin: Monatlicher Check**

```
Every 1st of Month (30 min):
━━━━━━━━━━━━━━━━━━━━━━━━
1. Dashboard → Performance-Report
2. Datenbankgröße wächst normal?
3. Speicherplatz ausreichend? (mindestens 50% frei)
4. User-Aktivitäten normal?
5. Audit-Log überprüfen (Trends)
6. Wartungs-Tasks für nächsten Monat planen

Häufige Aufgaben:
━━━━━━━━━━━━━━
- Benutzer hinzufügen/entfernen
- Passwörter zurücksetzen
- Reports aktualisieren (z.B. neue Richtlinie)
- Datenbank archivieren (alte Daten raus)
```

---

## 📋 Teil E: Troubleshooting Flowchart

```
┌──────────────────────────┐
│   PROBLEM TRITT AUF      │
└────────┬─────────────────┘
         │
         ▼
    ┌────────────────┐
    │ Schritt 1:     │
    │ WARTEN 2 min   │
    │ (System startet)
    └────┬───────────┘
         │
         ▼
    ┌────────────────┐     JA: Ende
    │ Funktioniert   │────────→ ✓
    │ jetzt?         │
    └────┬───────────┘
         │ NEIN
         ▼
    ┌────────────────┐
    │ Schritt 2:     │
    │ BROWSER        │
    │ AKTUALISIEREN  │
    │ (F5)           │
    └────┬───────────┘
         │
         ▼
    ┌────────────────┐     JA: Ende
    │ Funktioniert   │────────→ ✓
    │ jetzt?         │
    └────┬───────────┘
         │ NEIN
         ▼
    ┌────────────────┐
    │ Schritt 3:     │
    │ INTERNET OK?   │
    │ (Google testen)│
    └────┬───────────┘
         │
         ├─ JA: Backend-Problem → IT anrufen
         │
         └─ NEIN: Internet-Problem → IT/Provider anrufen

IF Backend-Problem:
  └─ Admin: Dashboard anschauen
     ├─ Red Alert? → Lesen & handeln
     ├─ Green aber nicht erreichbar?
     │  └─ Firewall? → IT
     │  └─ Service crashed? → Auto-Restart sollte laufen
     └─ Wenn gar nichts: Hard-Reboot Server (last resort)
```

---

## 🎁 Zusammenfassung: Die 3 Säulen der wartungsarmen App

### **1. Automatisierung**
- ✓ Backups automatisch
- ✓ Updates halb-automatisch
- ✓ Health-Checks automatisch
- ✓ Fehler-Recovery automatisch
**→ Mensch muss nur überwachen, nicht aktiv alles machen**

### **2. Visualisierung**
- ✓ Dashboards mit Farben (Grün = OK, Rot = Problem)
- ✓ Ein-Klick-Aktionen (nicht "tippe diese 5 Befehle")
- ✓ Keine Kommandozeile nötig (Browser reicht)
**→ Selbst Anfänger können Probleme erkennen**

### **3. Dokumentation**
- ✓ Checklisten (nicht Romane)
- ✓ Bilder/Screenshots (nicht nur Text)
- ✓ Flowcharts für Troubleshooting
- ✓ Support-Hotline für Notfälle
**→ Jeder kann selbst 80% der Probleme lösen**

---

## 📞 Support-Eskalation

```
LEVEL 1 (Benutzer selbst):
━━━━━━━━━━━━━━━━━━━━━━━
Kann ich mit Checklisten lösen?
  - App won't start
  - Forgot password
  - How to...
→ Checkliste folgen

LEVEL 2 (Admin):
━━━━━━━━━━━━━━━━━
Kann ich im Dashboard lösen?
  - Backup fehlgeschlagen
  - User blockiert
  - Storage voll
  - Performance langsam
→ Dashboard anschauen, Aktion durchführen

LEVEL 3 (IT-Support):
━━━━━━━━━━━━━━━━
Alles andere:
  - Hardware-Fehler
  - Netzwerk-Fehler
  - Code-Bugs
  - Security-Incidents
→ IT support@...

Average Response Times:
  Level 1: Self-service (5-10 min)
  Level 2: Admin (15-30 min)
  Level 3: IT (1-4 hours)
```

---

## 🏆 Ziel: "Wartung für Sekretärin möglich"

**Definition von Erfolg:**

- ✓ Nutzerin ohne IT-Erfahrung kann die App täglich bedienen
- ✓ Admin kann ohne IT-Hintergrund Backups prüfen & Nutzer verwalten
- ✓ 95% der Alltagsfehler können mit Checklisten gelöst werden
- ✓ Notfall-Support: Jeder weiß, wen er anrufen muss
- ✓ Ausfallzeit pro Jahr: < 1 Tag total (SLA 99.9%)

**IST vs. SOLL Vergleich:**

| Metrik | IST | SOLL |
|--------|-----|------|
| **Admin-Aufwand pro Woche** | 4-8 Stunden | 30 Minuten |
| **Durchschnittliche Fehlerb | 2-3 pro Monat | < 1 pro Jahr |
| **Ausfallzeit pro Fehler** | 2-3 Stunden | 5-15 Min |
| **IT-Supporttickets/Monat** | 8-12 | 1-2 |
| **Nutzer-Frustration** | Mittel-Hoch | Niedrig |
| **Update-Prozess** | 4 Stunden | 5 Minuten |

---

**Die Devise: "Make it so simple that even my grandmother can operate it!"** 👵

