# 🎬 Frauenhaus-App – Visuelle Übersicht & Use Cases

## 🎞️ Bildschirm-Flow (Was sieht der Benutzer?)

```
┌──────────────────┐
│   START / Batch  │  ← run_local.bat ausgeführt
└────────┬─────────┘
         │
         ▼
   ┌──────────────────────────┐
   │  Splash Screen (5 Sek)   │
   │   [LOGO]                 │
   │   Frauenhaus Verwaltung  │
   │   wird geladen...        │
   └────┬─────────────────────┘
        │
        ▼
   ┌────────────────────────────────────┐
   │   LOGIN-FENSTER                    │
   │   (CLoginFrame)                    │
   │                                    │
   │   Benutzer:   [dorle          ]    │
   │   Passwort:   [***           ]    │
   │                                    │
   │                [OK]   [Abbrechen]  │
   └────┬───────────────────────────────┘
        │
        │ ✓ Login erfolgreich
        │
        ▼
   ┌──────────────────────────────────────────────────┐
   │  HAUPTFENSTER (CMainFrame)                       │
   │  Frauenhaus Adress- und Bußgeldverwaltung        │
   ├──────────────────────────────────────────────────┤
   │  Tabs:                                           │
   │ [ Mitglieder ]  [ Spenden ]  [ Bußgelder ] ...│
   ├──────────────────────────────────────────────────┤
   │  Inhalt (je nach aktivem Tab):                  │
   │                                                  │
   │   ┌─────────────┐  ┌──────────────┐            │
   │   │  Mitglieder │  │   Spenden    │  ...       │
   │   │  verwalten  │  │  erfassen    │            │
   │   └─────────────┘  └──────────────┘            │
   │                                                  │
   │   ┌─────────────┐  ┌──────────────┐            │
   │   │ Bußgelder  │  │    Berichte  │            │
   │   │ verfolgen  │  │ generieren   │            │
   │   └─────────────┘  └──────────────┘            │
   │                                                  │
   │                                    [?] Hilfe    │
   └────────┬────────────────────────────────────────┘
            │
            │ Benutzer klickt z.B. "Mitglieder verwalten"
            │
            ▼
   ┌──────────────────────────────────────────────────┐
   │  LISTEN-FENSTER (CListFrame)                     │
   │  Mitglieder                                      │
   ├──────────────────────────────────────────────────┤
   │  [Neu] [Bearbeiten] [Löschen] [Kopieren] ...  │
   ├──────────────────────────────────────────────────┤
   │  ID │ Name      │ Vorname │ Telefon   │         │
   │  ───┼───────────┼─────────┼───────────┼         │
   │  1  │ Mueller   │ Maria   │ 06211...  │         │
   │  2  │ Schmidt   │ Anna    │ 06212...  │         │
   │  3  │ Becker    │ Petra   │ 06213...  │         │
   │     │           │         │           │         │
   │                                       │         │
   │                        [Schließen]   │         │
   └────┬──────────────────────────────────┘
        │
        │ Benutzer klickt [Neu] oder [Bearbeiten]
        │
        ▼
   ┌──────────────────────────────────────────────────┐
   │  DETAIL-FENSTER (CInfoFrame)                     │
   │  Neues Mitglied                                  │
   ├──────────────────────────────────────────────────┤
   │  Anrede:        [ Frau ▼]                       │
   │  Name:          [________________]              │
   │  Vorname:       [________________]              │
   │  Geburtsdatum:  [       /    /   ]              │
   │  Telefon:       [________________]              │
   │  Email:         [________________]              │
   │  Adresse:       [________________]              │
   │  Ort:           [________________]              │
   │  Stichworte:    [ ] Großspender                │
   │                 [ ] Newsletter                  │
   │                 [ ] ...                         │
   ├──────────────────────────────────────────────────┤
   │  [Speichern]  [Abbrechen]  [Zurücksetzen]       │
   └────┬───────────────────────────────────────────┘
        │
        │ Benutzer klickt [Speichern]
        │ → DB INSERT/UPDATE
        │
        │ oder [Berichte]
        │
        ▼
   ┌──────────────────────────────────────────────────┐
   │  REPORT-FENSTER (CReportFrame)                   │
   │  Report-Auswahl                                  │
   ├──────────────────────────────────────────────────┤
   │  Spendenübersicht:                              │
   │    Jahr: [2024        ▼]  [Ausführen]           │
   │                                                  │
   │  Bußgeldübersicht:                              │
   │    Zeitraum: [von] [01.01.2024]                 │
   │              [bis] [31.12.2024]  [Ausführen]   │
   │                                                  │
   │  Spendequittungen:                              │
   │    [Ausführen]                                   │
   │                                                  │
   │  [Schließen]                                     │
   └────┬───────────────────────────────────────────┘
        │
        │ Report wird generiert (Excel/Word)
        │
        ▼
   ┌──────────────────────────────────────────────────┐
   │  MICROSOFT EXCEL / WORD öffnet sich             │
   │  mit Report-Datei                                │
   │                                                  │
   │  Gespeichert unter: C:\frauenhaus\reports\      │
   │                                                  │
   │  Benutzer kann ausdrucken / weitergeben         │
   └──────────────────────────────────────────────────┘
```

---

## 🎭 Use Cases (Anwendungsfälle)

### **Use Case 1: Neues Mitglied erfassen**

```
┌─────────────────────┐
│  Sachbearbeiter     │
│  (Benutzer)         │
└──────────┬──────────┘
           │
           │ 1. Öffnet App
           │
           ▼
       ┌─────┐
       │ App │
       │(Hauptfenster)
       └──┬──┘
          │ 2. Klickt "Mitglieder → [Neu]"
          │
          ▼
       ┌─────────┐
    1. │Neues    │ Fenster öffnet
       │Mitglied │
       │Form     │
    2. │ Füllt  │◄── Name, Vorname, Telefon eingeben
       │aus      │
       └──┬──────┘
          │ 3. Klickt [Speichern]
          │
          ▼
       ┌─────────┐
       │   DB    │
       │INSERT   │◄── Neuer Datensatz in
       │         │    frauenhaus.mitglied
       └─────────┘
          │
          ▼
       ┌─────────┐
    1. │Erfolg   │ Mitteilung: "Mitglied erstellt"
    2. │Fenster  │ → Benutzer zurück zur Liste
       │schließt │
       └─────────┘
```

**Zeit:** ~2 Minuten  
**Daten:** Name, Vorname, Telefon (mind.), weitere optional  
**Speichert in:** `frauenhaus.mitglied`  

---

### **Use Case 2: Spende erfassen & Quittung ausstellen**

```
┌──────────────────┐
│   Spender/       │
│   Sachbearb.     │
└────────┬─────────┘
         │
         │ 1. Öffnet App
         │
         ▼
    ┌─────────────────────┐
    │ Hauptfenster        │
    │ → "Spenden"         │
    │ → [Neu]             │◄── Spende-Formular öffnet
    └──┬──────────────────┘
       │
       │ 2. Trägt ein:
       │    - Spender (Mitglied-Auswahl)
       │    - Betrag
       │    - Datum
       │    - Spendenart (Geldspende/Sachspende/...)
       │    - Verein (Frauenhaus/Förderverein)
       │
       ▼
    ┌─────────────┐
    │ DB INSERT   │
    │ →spende     │◄── Neue Spende gespeichert
    └──┬──────────┘
       │
       │ 3. Benutzer klickt [Report]
       │    "Spendequittung ausstellen"
       │
       ▼
    ┌─────────────────────────────────────────┐
    │ Report-Generator                        │
    │ CCommandSpendenQuittung / CReportSpendenQuittunge   │
    │                                         │
    │ - Lädt Word-Vorlage                     │◄── Vorlagen/
    │   (z.B. SpendenQuittungFrauenhaus...dot)│   SpendenQuittung*.dot
    │ - Ersetzt Platzhalter:                  │
    │   {Name} → "Mueller"                    │
    │   {Betrag} → "100 Euro"                 │
    │   {Datum} → "26.06.2024"                │
    │ - Speichert als PDF/DOC                 │
    └──┬──────────────────────────────────────┘
       │
       ▼
    ┌──────────────────────────┐
    │ Word/PDF öffnet sich     │
    │ mit Spendequittung       │
    │ (vor Benutzer)           │
    │                          │
    │ Benutzer:                │
    │ - Druckt aus             │
    │ - oder versendet per EMail
    └──────────────────────────┘
```

**Zeit:** ~5 Minuten  
**Daten:** Spender, Betrag, Datum, Spendenart  
**Speichert in:** `frauenhaus.spende`  
**Output:** PDF/Word Quittung  

---

### **Use Case 3: Jahresbericht mit Spendenübersicht**

```
Geschäftsführer / Bilanzierungsteam
         │
         │ "Ich brauch Bericht für Steuererklärung"
         │
         ▼
    ┌──────────────────────┐
    │ App → "Reports"      │
    │ → Spendenübersicht   │ Benutzer wählt Report-Typ
    └───┬──────────────────┘
        │
        │ Parameter-Dialog:
        │ [Jahr: 2024 ▼]
        │ [Typ: Alle ▼]
        │ [Verein: Alle ▼]
        │ [Ausführen]
        │
        ▼
    ┌───────────────────────────────────┐
    │ Backend: CReportSpendenUebersicht │
    │                                   │
    │ 1. Datenbankabfrage:              │
    │    SELECT s.verein,               │
    │           a.spendentyp,           │
    │           m.name,                 │
    │           s.datum,                │
    │           s.betrag                │
    │    FROM frauenhaus.spende s       │
    │    WHERE YEAR(s.datum) = 2024     │
    │    GROUP BY s.verein, a.spendentyp
    │                                   │
    │ 2. Excel-Template laden           │
    │    (Vorlagen/SpendenUebersicht.xls)
    │                                   │
    │ 3. Daten eintragen:               │
    │    ├─ Verein: Frauenhaus          │
    │    │  ├─ Geldspende:              │
    │    │  │  ├─ Mueller, Maria: 100€  │
    │    │  │  ├─ Schmidt, Anna: 50€    │
    │    │  │  └─ SUMME: 150€           │
    │    │  └─ Sachspende: ...          │
    │    └─ GESAMT-SUMME: 1.250€        │
    │                                   │
    │ 4. Speichern als:                 │
    │    reports/SpendenUebersicht.xls  │
    └───┬───────────────────────────────┘
        │
        ▼
    ┌─────────────────────────────────┐
    │ Microsoft Excel öffnet Datei    │
    │                                 │
    │ Benutzer kann:                  │
    │ - Ansehen                       │
    │ - Bearbeiten                    │
    │ - Ausdrucken                    │
    │ - Webarchivieren                │
    │ - An Steuererklärung anhängen   │
    └─────────────────────────────────┘
```

**Zeit:** ~10 Minuten  
**Eingaben:** Jahreszahl, Filteroptionen  
**Output:** Excel-Datei mit gruppierter Übersicht  
**Nutzen:** Transparenz für Verwaltungsrat, Steuererklärung, externe Prüfung  

---

### **Use Case 4: Bußgeldverwaltung & Nachverfolgung**

```
┌────────────────────────┐
│ Sachbearbeiter         │
│ "Bußgeld eingegangen"  │
└───────┬────────────────┘
        │
        ▼
    ┌──────────────────────────┐
    │ App → "Bußgelder"        │
    │ → [Neu]                  │◄── Formular öffnet
    └──┬───────────────────────┘
       │
       │ Trägt ein:
       │ - Gericht (Amtsgericht Mannheim)
       │ - Bußgeldbetrag: 500 €
       │ - Datum: 26.06.2024
       │ - Verein: Frauenhaus
       │ - Grund / Beschreibung
       │
       ▼
    ┌─────────────┐
    │ DB INSERT   │
    │ →bussgeld   │◄── Neue Bußgeld-Akte
    └──┬──────────┘
       │
       │ Später: "Zahlung eingegangen"
       │ Benutzer öffnet Bußgeld-Akte
       │ → [Bearbeiten] → Tab "Eingänge"
       │
       ▼
    ┌───────────────────────────┐
    │ "Eingang hinzufügen"      │
    │ - Eingangsdate: 26.06.2024│
    │ - Betrag: 500 €           │◄── Zahlung erfassen
    │ - [Speichern]             │
    └──┬────────────────────────┘
       │
       ▼
    ┌──────────────────────────┐
    │ DB INSERT                │
    │ → frauenhaus.eingang     │◄── Zahlung gespeichert
    └──┬───────────────────────┘
       │
       │ Report: Bußgeldübersicht
       │ (offenes + eingegangenes Bußgeld)
       │
       ▼
    ┌──────────────────────────────┐
    │ Excel-Report mit Übersicht:  │
    │                              │
    │ Gericht: Amtsgericht MM      │
    │ Bußgeldsum: 500€             │
    │ Eingänge:   500€             │
    │ Ausstehend: 0€ ✓             │
    └──────────────────────────────┘
```

**Zeit:** ~5–10 Minuten (Erfassung + Nachverfolgung)  
**Tabellen:** `frauenhaus.bussgeld`, `frauenhaus.eingang`, `frauenhaus.gericht`  
**Nutzen:** Finanzielle Transparenz, Mahnungszyklus  

---

## 📊 Datenfluss-Diagramm

```
┌────────────────────────────────────────────┐
│  BENUTZER                                  │
│  (Sachbearbeiter, Buchhalter, Admin)       │
└──────────┬─────────────────────────────────┘
           │ (Mauseingaben, Tastendrücke)
           │
           ▼
┌─────────────────────────────────────────┐
│  GUI (Java Swing)                       │
│  ├─ CMainFrame (Fenster-Verwaltung)   │
│  ├─ CListFrame (Datenlisten)           │
│  ├─ CInfoFrame (Formular-Editor)       │
│  └─ CReportFrame (Report-Auswahl)      │
└──────────┬──────────────────────────────┘
           │ (Benutzerintentionen)
           │
           ▼
┌─────────────────────────────────────────┐
│  Geschäftslogik (Business Layer)       │
│  ├─ CDataManager (Zentral-Verwaltung)  │
│  ├─ CListDataManaging* (Listen-Handler)│
│  ├─ CInfoDataManaging* (Formular-Handler)
│  └─ CReport* (Report-Generator)        │
└──────────┬──────────────────────────────┘
           │ (SQL-Befehle + SQL-Parameter)
           │
           ▼
┌─────────────────────────────────────────┐
│  Datenzugriff (Data Access Layer)      │
│  ├─ CManagingSQLServer (DB-Treiber)   │
│  └─ JDBC + jTDS-Connector              │
└──────────┬──────────────────────────────┘
           │ (TCP Port 1433, SQL Server Protocol)
           │
           ▼
┌─────────────────────────────────────────┐
│  Microsoft SQL SERVER                  │
│  ├─ [frauenhaus] Schema                │ ◄── Geschäftsdaten
│  │  ├─ mitglied                        │
│  │  ├─ spende                          │
│  │  ├─ bussgeld                        │
│  │  └─ ...                             │
│  │                                     │
│  └─ [compucrash] Schema                │ ◄── Framework-Config
│     ├─ user_def                        │
│     ├─ object_def                      │
│     └─ ...                             │
└─────────────────────────────────────────┘
           │ (ResultSets, Metadaten)
           │
           ▼
      ┌─────────────┐
      │  Back-Flow   │
      │ (Anzeige von │ ▲ ← Daten durch GUI
      │ Ergebnissen) │ │ angezeigt
      └──────┬──────┘ │
             └────────┘
```

---

## 🎪 Zusammenfassung: "Die 5 Hauptfunktionen"

```
┌─────────────────────────────────────────────────────────────┐
│  Die Frauenhaus-App hat 5 Hauptfunktionen:                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1️⃣  BENUTZENDE & AUTHENTIFIZIERUNG                        │
│     → Login mit Benutzername/Passwort                      │
│     → Rollenbasierte Zugriffe (wer darf was?)             │
│     → Gespeichert in: compucrash.user_def                 │
│                                                             │
│  2️⃣  DATENVERWALTUNG (CRUD-Operationen)                   │
│     → Anlegen (Create)    → [Neu]                          │
│     → Lesen (Read)        → [Anzeigen]                     │
│     → Bearbeiten (Update) → [Bearbeiten]                   │
│     → Löschen (Delete)    → [Löschen]                      │
│     → Kopieren (Duplicate)→ [Kopieren]                     │
│     → Gespeichert in: frauenhaus.mitglied/spende/...      │
│                                                             │
│  3️⃣  DATENFILTERUNG                                        │
│     → Suche nach Stichworten                               │
│     → Filter nach Datum, Bereich, Typ                      │
│     → Sortieren & Gruppieren                               │
│     → Gespeichert in: frauenhaus.stichwort                │
│                                                             │
│  4️⃣  REPORT-GENERIERUNG (Excel/Word)                       │
│     → Spendenübersicht pro Jahr                            │
│     → Spendequittungen für Steuern                         │
│     → Bußgeldübersicht pro Gericht                         │
│     → Serienbriefe & Adressetiketten                       │
│     → Gespeichert in: C:\frauenhaus\reports\              │
│                                                             │
│  5️⃣  MAIL-MERGE & DOKUMENTGENERIERUNG                      │
│     → Briefvorlagen laden (.dot Dateien)                   │
│     → DB-Felder in Vorlage einfügen                        │
│     → Als PDF/Word speichern & ggf. versenden             │
│     → Gespeichert in: vorlagen\ (*.dot Dateien)           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

**Das war's! Die App ist im Grunde ein "klassisches CRUD-Verwaltungssystem mit Reporting & Document-Generation". Nichts Hochkompliziertes, aber bewährt seit ~15 Jahren! 📊**

