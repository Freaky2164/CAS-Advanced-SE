# 📚 Dokumentations-Übersicht – Für wen ist was?

## 🎯 Schnelle Navigation

Clck auf deinen Bereich unten und folge der Dokumentation:

---

## 👤 Für den **Benutzer** (Sachbearbeiter, Kassiererin, Secretary)

```
Deine Aufgabe: Täglich mit der App arbeiten

START HIER:
┣─ QUICKSTART.md
│  └─ 5-Minuten Setup (wenn neu)
│
├─ FUNKTIONSWEISE_VISUAL.md
│  └─ Was tut die App wirklich? (Bilder!)
│
├─ Checklisten/TAEGLICHE_VERFUEGBARKEITSPRUEFUNG.txt
│  └─ Morning-Check (5 Min, vor Arbeit)
│
└─ FEHLER_...md (bei Problemen)
   ├─ FEHLER_ADOBE_READERPREFS
   ├─ FEHLER_SQLSERVER_VERBINDUNG
   └─ SCHNELLFIX_SQLSERVER

HÄUFIGE FRAGEN:
  ❓ "Wie starte ich die App?"
     → QUICKSTART.md Schritt 1-2
  
  ❓ "Was tut die App?"
     → FUNKTIONSWEISE_VISUAL.md
  
  ❓ "Sie geht nicht!"
     → SCHNELLFIX_SQLSERVER.md
```

---

## 🛠️ Für den **Administrator** (Schülüssel-Person, IT-Hauswart)

```
Deine Aufgabe: Das System läuffähig halten, Benutzer verwalten

START HIER:
┣─ ADMIN_HANDBUCH.md ⭐ PRIMARY REFERENCE
│  ├─ Wöchentliche Aufgaben (Checkliste)
│  ├─ Backup-Überwachung
│  ├─ User Management
│  ├─ Notfall-Szenarien
│  └─ Troubleshooting Flowchart
│
├─ WARTUNG_ANFAENGER.md
│  └─ Tiefere Erklärungen der Konzepte
│
├─ Checklisten/
│  ├─ TAEGLICHE_VERFUEGBARKEITSPRUEFUNG.txt
│  └─ [Weitere Checklisten - Ausdruckbar!]
│
└─ NEUE_ARCHITEKTUR_WARTUNGSARM.md
   └─ Die Zukunft verstehen (langfristig planen)

HÄUFIGE AUFGABEN (Find in ADMIN_HANDBUCH):
  ❓ "Tägliche Routine?"
     → Abschnitt "WÖCHENTLICHES ADMIN-RITUAL"
  
  ❓ "Benutzer hinzufügen?"
     → Abschnitt "User Management"
  
  ❓ "Backup fehlgeschlagen?"
     → Abschnitt "NOTFALL-SZENARIEN" → Szenario 2
  
  ❓ "Die App ist langsam!"
     → Abschnitt "Szenario 3: Performance"
```

---

## 👨‍💼 Für die **Geschäftsführung** (Management, Entscheider)

```
Deine Aufgabe: Strategische Entscheidungen treffen

START HIER:
┣─ ARCHITEKTUR_NEU.md ⭐ PRIMARY REFERENCE
│  ├─ Warum neue Architektur? (Was sind die Probleme?)
│  ├─ Was wird besser? (Features)
│  ├─ Kosten-Nutzen? (ROI)
│  └─ Sicherheit? (Compliance)
│
├─ NEUE_ARCHITEKTUR_WARTUNGSARM.md
│  ├─ Vergleich IST vs. SOLL
│  ├─ ROI-Berechnung
│  ├─ Business Impact
│  └─ Migrationsplan (Phasen & Kosten)
│
└─ FUNKTIONSWEISE.md
   └─ Technical Overview (was das System tut)

EXECUTIVE SUMMARY (Kurz & Knackig):
  
  Aktuell (IST):
    ❌ Installation auf jedem PC
    ❌ Jeder Fehler = 2-3h IT-Support
    ❌ Manuelle Backups
    ❌ Update dauert halben Tag
    ❌ Kosten: ~8,560€/Jahr
  
  Neu (SOLL):
    ✅ Browser-Lösung (keine Installation)
    ✅ Automatisch Fehler-Recovery
    ✅ Automatische Backups
    ✅ Updates = 5 Minuten
    ✅ Kosten: ~1,840€/Jahr (nach Amortisation)
    ✅ ROI: Break-Even nach 4 Jahren
    ✅ Danach: 78% kostenreduktion!

ENTSCHEIDUNGSFRAGEN:
  ❓ "Was kostet es?"
     → NEUE_ARCHITEKTUR_WARTUNGSARM.md → ROI-Berechnung
  
  ❓ "Wie lange dauerts?"
     → ARCHITEKTUR_NEU.md → Migrationsplan (13 Wochen)
  
  ❓ "Sicher?"
     → ARCHITEKTUR_NEU.md → Datensicherheit (Abschnitt 3)
  
  ❓ "Warum jetzt?"
     → ARCHITEKTUR_NEU.md → IST Schwachstellen (Abschnitt 1.3)
```

---

## 💻 Für **Entwickler/IT** (wenn eigenes Team)

```
Deine Aufgabe: Code schreiben, System architektur aufbauen

START HIER:
┣─ ARCHITEKTUR_NEU.md ⭐ PRIMARY REFERENCE
│  ├─ 3-Schichten-Architektur
│  ├─ Tech Stack (Spring Boot, Angular, SQL Server)
│  ├─ Security Details (TDE, JWT, Prepared Statements)
│  ├─ Backup/Recovery-Strategie
│  └─ Migrationsplan
│
├─ FUNKTIONSWEISE.md
│  └─ Bestehende Business-Logik verstehen
│
├─ FUNKTIONSWEISE_VISUAL.md
│  └─ Workflows & Use-Cases (für Datenmodell)
│
└─ WARTUNG_ANFAENGER.md
   └─ "Operations" Anforderungen (Monitoring, etc)

ENTWICKLUNGS-FOKUS:

Phase 1: Datenbank
  - SQL Server Setup (TDE, Backup-Jobs)
  - Flyway Migrations
  - Schema-Upgrade
  
Phase 2: Backend
  - Spring Boot Projekt
  - REST Endpoints
  - Security (Spring Security + JWT)
  - Die Queries (from CManagingDatabase in Java reimplement)
  
Phase 3: Frontend
  - Angular SPA
  - UI-Komponenten
  - State Management
  
Phase 4: DevOps
  - Windows Service (WinSW)
  - Docker-Build (optional)
  - Health Checks & Monitoring
  
Phase 5: Testing & Go-Live
  - Unit Tests (JUnit5)
  - Integration Tests
  - Pentest (Security)
  - UAT (User Acceptance Test)

MIGRATIONS-STRATEGIE:
  - Parallel-Betrieb während Migration
  - Data-Export vom alten → Import in neuem System
  - Dual-Write Pattern (alte + neue DB gleichzeitig)
  - Cutover nach 2 Wochen Testing
```

---

## 📋 Alle Dateien Diese aufgelistet

```
FH_MA (Hauptverzeichnis)
│
├─ 📄 ARCHITEKTUR_NEU.md ⭐⭐⭐
│  └─ Fokus: IST-Analyse + Neue Lösung (Geschäftsführung & Developer)
│
├─ 📄 FUNKTIONSWEISE.md ⭐⭐
│  └─ Was tut die aktuelle App? (Technischer Überblick)
│
├─ 📄 FUNKTIONSWEISE_VISUAL.md ⭐⭐
│  └─ Use-Cases & Workflows mit Bildern
│
├─ 📄 QUICKSTART.md ⭐
│  └─ 5-Minuten Setup für Erste Schritte
│
├─ 📄 STARTUP_ANLEITUNG.md ⭐
│  └─ Detaillierte Startup-Dokumentation
│
├─ 📄 WARTUNG_ANFAENGER.md ⭐⭐
│  └─ Wie macht man Wartung leicht? (Core Strategy)
│
├─ 📄 ADMIN_HANDBUCH.md ⭐⭐⭐
│  └─ Checklisten für Admin (Täglich/Wöchentlich/Monatlich)
│
├─ 📄 NEUE_ARCHITEKTUR_WARTUNGSARM.md ⭐⭐
│  └─ IST vs. SOLL Vergleich + ROI
│
├─ 📄 FEHLER_ADOBE_READERPREFS.md
│  └─ Troubleshooting: Adobe-Fehler beim Start
│
├─ 📄 FEHLER_SQLSERVER_VERBINDUNG.md
│  └─ Troubleshooting: SQL Server ist nicht erreichbar
│
├─ 📄 SCHNELLFIX_SQLSERVER.md
│  └─ 5-Minuten Fix für DB-Probleme
│
└─ Z/ (Verzeichnis mit Skripten & Konfiguration)
   ├─ run_local.bat ⭐ (für Nutzer: App starten)
   ├─ compile.bat (für Entwickler: Neu kompilieren)
   ├─ localhost.ini (Konfigurationsdatei)
   ├─ diagnose_sqlserver.bat (SQL Server Check)
   ├─ Restore-Database.ps1 (DB wiederherstellen)
   └─ CStart.java (korrigierter Source)

Checklisten/ (Ausdruckbare Vorlagen)
   └─ TAEGLICHE_VERFUEGBARKEITSPRUEFUNG.txt
```

---

## 🎯 Empfohlene Lese-Reihenfolge

### **Wenn total neu (nie gesehen):**
```
1. QUICKSTART.md (10 Min)
   → "App starten" verstehen
   
2. FUNKTIONSWEISE_VISUAL.md (15 Min)
   → "Was die App macht" sehen
   
3. ADMIN_HANDBUCH.md (30 Min)
   → "Wie betreibe ich das" verstehen
```

### **Wenn Migration planen (Geschäftsführung):**
```
1. ARCHITEKTUR_NEU.md → Abschnitt 1 (IST-Probleme) (20 Min)
2. ARCHITEKTUR_NEU.md → Abschnitt 2 (SOLL-Lösung) (20 Min)
3. NEUE_ARCHITEKTUR_WARTUNGSARM.md → ROI-Berechnung (15 Min)
4. ARCHITEKTUR_NEU.md → Section 8 (Migrationsplan) (15 Min)
```

### **Wenn entwickeln (Tech-Team):**
```
1. FUNKTIONSWEISE.md (30 Min)
   → Alte Business-Logik verstehen
   
2. FUNKTIONSWEISE_VISUAL.md (20 Min)
   → Workflows begreife
   
3. ARCHITEKTUR_NEU.md (60 Min)
   → Neue Architektur lernen
   
4. ARCHITEKTUR_NEU.md → Section 6-7 (Tech Stack & Tests) (30 Min)
```

---

## 🚨 Notfall-Zugriff (Schnelle Links)

```
App geht nicht?
  → Z/SCHNELLFIX_SQLSERVER.md

Fehler beim Start?
  → Z/FEHLER_ADOBE_READERPREFS.md
  
Admin weiß nicht was tun?
  → ADMIN_HANDBUCH.md → Troubleshooting Flowchart

Business fragt "Warum teuer?":
  → NEUE_ARCHITEKTUR_WARTUNGSARM.md → ROI-Berechnung

IT fragt "Wie programmiert man das?":
  → ARCHITEKTUR_NEU.md + FUNKTIONSWEISE.md
```

---

## 📞 Kontakte (ausfüllen!)

```
SUPPORT HIERARCHY:
━━━━━━━━━━━━━━━━
Nutzer-Problem:
  ↓
Admin versucht mit Checkliste
  ↓ (wenn Admin können mit Checkliste lösen)
  ✓ LÖSUNG
  ↓ (wenn Admin NICHT lösen können)
  IT-Support anrufen:
  Tel: +49 _________________
  Email: ___________________
  
CRITICAL HOTLINE (nach Stunden):
  Tel: +49 _________________
  
GESCHÄFTSFÜHRER (Super-Notfall):
  Tel: +49 _________________
```

---

**Das war's! Mit diesen Dokumenten hat JEDER die richtige Information für sie Aufgabe!** 🎉

Bevölt einfach die relevante Datei, je nach Rolle. Wenn Fragen auftauchen: Suche in der obigen Liste!
