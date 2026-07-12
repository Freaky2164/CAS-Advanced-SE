# 📋 Funktionsweise der Frauenhaus-Verwaltungsanwendung

## 🎯 Was ist diese Anwendung?

Die **Frauenhaus Adress- und Bußgeldverwaltung** ist ein **Datenverwaltungssystem für gemeinnützige Organisationen** – speziell für:
- Ein Frauenhaus (Schutzeinrichtung für Frauen)
- Ein angegliederten Förderverein

Die Anwendung verwaltet:
- **Kontaktdaten** (Mitglieder, Spender, Partner)
- **Spendenverwaltung** (wer spendet, wie viel, wann)
- **Bußgeldverwaltung** (verhängte Bußgelder gegen das Frauenhaus?)
- **Dokumentengenerierung** (Briefe, Quittungen, Etiketten)
- **Berichtswesen** (Reports, Übersichten für Verwaltung)

---

## 🏗️ Architektur – Wie funktioniert sie?

```
┌─────────────────────────────────────────────────────────────┐
│                   BENUTZER (am PC)                          │
└────────────────┬────────────────────────────────────────────┘
                 │
         (Java Swing GUI)
                 │
    ┌────────────▼─────────────────────┐
    │  CMainFrame (Hauptfenster)       │
    │  ├─ CListFrame (Datenlisten)     │◄──── Anblick der Daten
    │  ├─ CInfoFrame (Detail-Editor)   │
    │  └─ CReportFrame (Report-Wahl)   │
    └────────────┬─────────────────────┘
                 │
         (Business Logic)
                 │
    ┌────────────▼──────────────────────┐
    │  CDataManager (Datenverwaltung)  │
    │  ├─ CDataObjectFactory           │◄──── Basis-Operationen
    │  ├─ CListDataManaging*           │      (CRUD)
    │  └─ CInfoDataManaging*           │
    └────────────┬──────────────────────┘
                 │
         (Datenbankzugriff)
                 │
    ┌────────────▼────────────────────────────┐
    │  CManagingSQLServer                    │
    │  (JDBC → jTDS Treiber)                 │
    └────────────┬────────────────────────────┘
                 │
                 │ TCP Port 1433
                 ▼
    ┌──────────────────────────────────────┐
    │  MS SQL Server                       │
    │  ├─ [frauenhaus] Schema              │◄──── Daten
    │  │  ├─ mitglied (Personen)           │      Persistie-
    │  │  ├─ spende  (Spenden)             │      rung
    │  │  ├─ bussgeld (Bußgelder)          │
    │  │  └─ eingang (Eingänge)            │
    │  │                                    │
    │  └─ [compucrash] Schema              │      UI-Konfigu-
    │     ├─ object_def (UI-Objekte)       │      ration
    │     ├─ button_def (GUI-Buttons)      │
    │     └─ user_def (Benutzer)           │
    └──────────────────────────────────────┘
```

---

## 🎮 Die Hauptkomponenten

### 1. **Startup & Login** (CStart → CLoginFrame)

**Was passiert:**

```
Benutzer führt aus: run_local.bat
         ↓
   compucrash.CStart startet
         ↓
   App liest localhost.ini (Konfiguration)
         ↓
   CPropertyManager lädt Einstellungen
         ↓
   CDataManager verbindet sich mit SQL Server
         ↓
   CLoginFrame wird angezeigt
         ↓
   Benutzer gibt Anmeldedaten ein
         ↓
   App prüft in compucrash.user_def Tabelle
         ↓
   Login erfolgreich → CMainFrame wird angezeigt
```

**Datenbankschema für Login:**
```sql
-- compucrash.user_def (UI-Framework-Tabelle)
SELECT user_name, password, main_frame FROM compucrash.user_def;
-- Beispiel:
-- dorle | <hashed_pw> | CMainFramePersonal
```

---

### 2. **Hauptfenster** (CMainFrame)

**Aufgabe:** Zentrale Navigations-Hub mit Tabs und Funktionsschaltflächen

**Struktur:**

```
┌─────────────────────────────────────────────────────┐
│ Frauenhaus Adress und Bußgeldverwaltung [Logo]    │
├─────────────────────────────────────────────────────┤
│  [ Mitglieder ]  [ Spenden ]  [ Bußgelder ] [...] │  ← Tabs
├─────────────────────────────────────────────────────┤
│                                                     │
│   ┌──────────────┐  ┌──────────────┐              │
│   │  Mitglieder  │  │   Spenden    │  ...         │  ← Funktions-buttons
│   │  verwalten   │  │  erfassen    │              │     (300x120px)
│   └──────────────┘  └──────────────┘              │
│                                                     │
│                                                     │ ← Inhalt je Tab
│                                                     │
├─────────────────────────────────────────────────────┤
│ ?  [ Hilfe ]                                        │  ← Menu
└─────────────────────────────────────────────────────┘
```

**Datenbank-Konfiguration für Tabs/Buttons:**
```sql
-- Tabellen aus compucrash-Schema
SELECT * FROM compucrash.object_def;        -- Verfügbare Objekte
SELECT * FROM compucrash.user_object_relation;  -- Benutzer → Objekte
SELECT * FROM compucrash.cust_button_main_rel;  -- Custom Buttons
```

---

### 3. **Listen-Ansicht** (CListFrame)

**Aufgabe:** Zeige alle Datensätze einer Tabelle (z.B. alle Mitglieder) in Tabellenform

**Was der Benutzer sieht:**

```
┌────────────────────────────────────────────────────┐
│ [Neu] [Bearbeiten] [Löschen] [Kopieren] [Anzeigen] │
├────────────────────────────────────────────────────┤
│ ID  │ Name        │ Vorname   │ Telefon          │
├─────┼─────────────┼───────────┼──────────────────┤
│ 1   │ Mueller     │ Maria     │ 06211/123456     │
│ 2   │ Schmidt     │ Anna      │ 06211/234567     │
│ 3   │ Becker      │ Petra     │ 06211/345678     │
│ ... │ ...         │ ...       │ ...              │
├────────────────────────────────────────────────────┤
│                                         [Schließen] │
└────────────────────────────────────────────────────┘
```

**Code-Flow:**
```java
// CListFrame erzeugt sich selbst
CListFrame listFrame = new CListFrame("mitglied", parentFrame);

// Datenladen aus DB
CListDataManagingSQLServer dataMgr = new CListDataManagingSQLServer(...);
ResultSet rset = dataMgr.select(...);  // SQL: SELECT ... FROM frauenhaus.mitglied

// Tabelle füllen
CTable tab = new CTable();
tab.setModel(rset);  // Zeigt Daten in JTable an

// Buttons definieren (aus Datenbank)
Neu:       bNew → create new record
Bearbeiten: bEdit → open CInfoFrame für selected row
Löschen:   bDelete → delete selected row
Kopieren:  bCopy → duplicate selected row
Anzeigen:  bDisplay → show read-only version
```

**Entsprechende Datenbanktabelle:**
```sql
-- Die Mitglieder-Tabelle (frauenhaus.mitglied)
SELECT mitglied, name, vorname, telefon, ... 
FROM frauenhaus.mitglied
WHERE 1=1;
```

---

### 4. **Detail-Editor** (CInfoFrame)

**Aufgabe:** Bearbeite einen einzelnen Datensatz (z.B. eine Person)

**Was der Benutzer tut:**

1. **Neu:** Klick auf [Neu] in der Liste
2. **Form öffnet sich:**
   ```
   ┌─────────────────────────────────────────┐
   │ Neues Mitglied                          │
   ├─────────────────────────────────────────┤
   │ Anrede:         [ - ]                   │  ← Dropdown
   │ Name:           [              ]        │  ← Textfeld
   │ Vorname:        [              ]        │
   │ Geburtsdatum:   [              ]        │  ← Datumspicker
   │ Telefon:        [              ]        │
   │ Email:          [              ]        │
   │ Adresse:        [              ]        │
   │ Ort:            [              ]        │
   │ ...                                     │
   ├─────────────────────────────────────────┤
   │ [ Speichern ]  [ Abbrechen ]            │
   └─────────────────────────────────────────┘
   ```

3. **Speichern:** Sql INSERT oder UPDATE wird ausgeführt

**Datenbank-Konfiguration für Formularfelder:**
```sql
-- object_tab_col_def: Beschreibt Felder des Formulars
SELECT 
    column_name,        -- z.B. "name"
    label,              -- z.B. "Name"
    data_type,          -- z.B. "CHAR", "DATE"
    editable,           -- 1 oder 0
    source              -- z.B. Dropdown-Werte-Tabelle
FROM compucrash.object_tab_col_def
WHERE object_name = 'mitglied';
```

---

### 5. **Reports & Berichte** (CReportFrame, CReportSpendenUebersicht, etc.)

**Aufgabe:** Generiere Excel-Berichte, Briefe, Quittungen

**verfügbare Reports:**

| Report | Beschreibung | Ausgang |
|--------|-------------|---------|
| **Spendenübersicht** | Alle Spenden pro Jahr, gruppiert | `reports\SpendenUebersicht.xls` |
| **Bußgeldübersicht** | Bußgelder pro Gericht, Zeitraum | `reports\Bußgeldübersicht.xls` |
| **Spendequittungen** | Quittungen für Steuererklärungen | `reports\SpendenQuittungen.xls` |
| **Serienbrief** | Mail-Merge (z.B. Newsletter) | `reports\Serienbrief.xls` |
| **Stichwortsuche** | Volltextsuche in Datenbank | `reports\StichwortSuche.xls` |
| **Verteiler** | Adresslisten | `reports\Verteiler.xls` |

**Report-Beispiel (SpendenUebersicht):**

```java
public class CReportSpendenUebersicht extends CCommand implements CReport {
    public void go() {
        // 1. Daten aus DB laden
        String sql = "SELECT s.verein, a.spendentyp, s.spendenart, "
                   + "m.name, m.vorname, s.datum, s.betrag "
                   + "FROM frauenhaus.mitglied m, frauenhaus.spende s, "
                   + "frauenhaus.spendenart a "
                   + "WHERE s.mitglied = m.mitglied "
                   + "AND s.spendenart = a.spendenart "
                   + "ORDER BY s.verein, a.spendentyp, s.spendenart";
        
        // 2. Excel-Template laden
        POIFSFileSystem fsin = new POIFSFileSystem(
            new FileInputStream("vorlagen/SpendenUebersicht.xls"));
        HSSFWorkbook wb = new HSSFWorkbook(fsin);
        HSSFSheet sheet = wb.getSheetAt(0);
        
        // 3. Daten in Excel eintragen
        int line = 0;
        while (rset.next()) {
            HSSFRow row = sheet.createRow(line++);
            row.createCell(0).setCellValue(rset.getString("name"));
            row.createCell(1).setCellValue(rset.getString("vorname"));
            row.createCell(2).setCellValue(rset.getDate("datum"));
            row.createCell(3).setCellValue(rset.getDouble("betrag"));
        }
        
        // 4. Speichern und öffnen
        FileOutputStream fileOut = new FileOutputStream(
            "reports/SpendenUebersicht.xls");
        wb.write(fileOut);
        fileOut.close();
        
        // 5. In Excel öffnen
        Runtime.getRuntime().exec(
            "C:\\Program Files (x86)\\Microsoft Office\\Office14\\excel.exe "
            + "reports/SpendenUebersicht.xls");
    }
}
```

---

## 📊 Datenbankschema (Vereinfacht)

### **Geschäftsdaten** (frauenhaus Schema)

```sql
-- Personen / Mitglieder
CREATE TABLE frauenhaus.mitglied (
    mitglied   INT PRIMARY KEY,
    name       VARCHAR(100),
    vorname    VARCHAR(100),
    telefon    VARCHAR(20),
    email      VARCHAR(100),
    adresse    VARCHAR(200),
    ort        VARCHAR(100)
);

-- Spenden (wer spendet was wann)
CREATE TABLE frauenhaus.spende (
    spende     INT PRIMARY KEY,
    mitglied   INT FOREIGN KEY,  -- Link zu mitglied
    datum      DATE,
    betrag     DECIMAL(10,2),
    spendenart INT,              -- Link zu spendenart
    verein     VARCHAR(50)        -- "Frauenhaus" oder "Förderverein"
);

-- Spendenarten (Kategorien)
CREATE TABLE frauenhaus.spendenart (
    spendenart INT PRIMARY KEY,
    bez        VARCHAR(100),     -- z.B. "Geldspende", "Sachspende"
    spendentyp VARCHAR(50)       -- z.B. "Einmalspende", "Dauerspende"
);

-- Bußgelder
CREATE TABLE frauenhaus.bussgeld (
    bussgeld   INT PRIMARY KEY,
    gericht    INT,              -- Link zu gericht
    betrag     DECIMAL(10,2),
    datum      DATE,
    verein     VARCHAR(50)       -- "Frauenhaus" oder "Förderverein"
);

-- Eingänge (Zahlungen zu Bußgelderm)
CREATE TABLE frauenhaus.eingang (
    eingang    INT PRIMARY KEY,
    bussgeld   INT FOREIGN KEY,  -- Link zu bussgeld
    betrag     DECIMAL(10,2),
    datum      DATE
);

-- Gerichte
CREATE TABLE frauenhaus.gericht (
    gericht    INT PRIMARY KEY,
    bez        VARCHAR(100)      -- z.B. "Amtsgericht Mannheim"
);

-- Vereine
CREATE TABLE frauenhaus.verein (
    verein     VARCHAR(50) PRIMARY KEY,
    name       VARCHAR(100)      -- "Frauenhaus Mannheim" oder
                                  -- "Förderverein Frauenhaus"
);

-- Anrede (Titel)
CREATE TABLE frauenhaus.anrede (
    anrede     INT PRIMARY KEY,
    bez        VARCHAR(50)       -- "Frau", "Herr", "Familie"
);

-- Stichworte (Tags/Kategorien für Personen)
CREATE TABLE frauenhaus.stichwort (
    stichwort  INT PRIMARY KEY,
    bez        VARCHAR(100)      -- z.B. "Großspender", "Newsletter"
);

-- Person-Stichwort-Zuordnung (n:m Beziehung)
CREATE TABLE frauenhaus.person_stichwort (
    mitglied   INT FOREIGN KEY,
    stichwort  INT FOREIGN KEY
);
```

### **Framework-Konfiguration** (compucrash Schema)

```sql
-- Benutzer
CREATE TABLE compucrash.user_def (
    user_name  VARCHAR(50) PRIMARY KEY,
    password   VARCHAR(100),
    main_frame VARCHAR(100)     -- z.B. "CMainFramePersonal"
);

-- Objekte (was wird in der UI verwaltet?)
CREATE TABLE compucrash.object_def (
    object_name  VARCHAR(100) PRIMARY KEY,
    object_label VARCHAR(100)  -- z.B. "Mitglieder"
);

-- Benutzer → Objekt Zuordnung (Rechte)
CREATE TABLE compucrash.user_object_relation (
    user_name   VARCHAR(50),
    object_name VARCHAR(100),
    panel       VARCHAR(50),    -- Tab-Zugehörigkeit
    bnew        BIT,            -- Berechtigung: Neu
    bedit       BIT,            -- Berechtigung: Bearbeiten
    bdelete     BIT,            -- Berechtigung: Löschen
    bcopy       BIT,            -- Berechtigung: Kopieren
    bdisplay    BIT             -- Berechtigung: Anzeigen
);

-- Objekttabellen (auf welche DB-Tabelle bezieht sich das Objekt?)
CREATE TABLE compucrash.object_tab_def (
    object_name  VARCHAR(100),
    owner        VARCHAR(50),   -- Schema, z.B. "frauenhaus"
    table_name   VARCHAR(100),  -- Tabellenname, z.B. "mitglied"
    isleading    BIT            -- Ist es die Haupttabelle?
);

-- Tabellenspalten-Definition (Formularfelder)
CREATE TABLE compucrash.object_tab_col_def (
    object_name  VARCHAR(100),
    owner        VARCHAR(50),
    table_name   VARCHAR(100),
    column_name  VARCHAR(100),
    label        VARCHAR(100),  -- Sichtbar im Formular als Label
    data_type    VARCHAR(50),   -- CHAR, INT, DATE, DECIMAL
    editable     BIT,           -- Bearbeitbar (0/1)?
    pos_info     INT,           -- Position im Detail-Formular
    pos_list     INT,           -- Position in der Listenansicht
    source       VARCHAR(100),  -- Für Dropdowns: Quell-Tabelle
    data_length  INT,           -- Max. Feldlänge
    tooltip      VARCHAR(200)   -- Hilfetext
);

-- Buttons (was kann man in der UI klicken?)
CREATE TABLE compucrash.button_def (
    buttonid    INT PRIMARY KEY,
    bez         VARCHAR(100),   -- z.B. "Speichern", "Abbrechen"
    label       VARCHAR(100),
    command     VARCHAR(100),   -- Java-Klassenname zum Ausführen
    image       VARCHAR(100)    -- Icon (wenn vorhanden)
);

-- Custom Button → Objekt Zuordnung
CREATE TABLE compucrash.cust_button_rel (
    buttonid    INT,
    user_name   VARCHAR(50),
    dialog_type VARCHAR(50),    -- "info" oder "list"
    object_name VARCHAR(100),
    panel       VARCHAR(50),
    pos         INT             -- Position im Dialog
);
```

---

## 🔄 Typische Workflows

### **Workflow 1: Neues Mitglied erfassen**

```
Benutzer wählt in Hauptfenster
→ "Mitglieder" → [Neu]
      ↓
CListFrame.bNew() wird aufgerufen
      ↓
CInfoFrame wird geöffnet (leer)
      ↓
Benutzer ausfüllen:
  - Anrede
  - Name
  - Vorname
  - Telefon
  - Email
  - etc.
      ↓
Benutzer klickt [Speichern]
      ↓
CInfoFrame → INSERT in frauenhaus.mitglied
      ↓
Erfolgreich: CListFrame wird aktualisiert
```

**SQL dahinter:**
```sql
INSERT INTO frauenhaus.mitglied 
(mitglied, name, vorname, telefon, email, ...)
VALUES (
    (SELECT MAX(mitglied)+1 FROM frauenhaus.mitglied),
    'Mueller',
    'Maria',
    '06211/123456',
    'maria@example.de',
    ...
);
```

---

### **Workflow 2: Spende abrechnen**

```
1. Benutzer öffnet "Spenden"
   → Liste aller Spenden wird loaded
   
2. Benutzer filtert nach Jahr / Typ
   → Datenbankfilter wird angewendet
   
3. Benutzer klickt [Report]
   → CReportSpendenUebersicht.go() wird aufgerufen
   
4. App generiert Excel:
   - Gruppiert nach Verein (Frauenhaus / Förderverein)
   - Sortiert nach Spendentyp und Spendenart
   - Berechnet Summen
   
5. Excel öffnet sich in Microsoft Excel
   → Benutzer kann ausdrucken / weitergeben
```

---

### **Workflow 3: Briefe / Quittungen generieren**

```
CReportSpendenQuittung / CCommandBriefFrauenhaus
      ↓
1. Word-Vorlage laden (.dot Datei aus vorlagen/)
   z.B. "SpendenQuittungFrauenhausgeldspende.dot"
      ↓
2. Mail-Merge mit Daten aus DB:
   - SELECT m.name, m.vorname, s.betrag, s.datum
     FROM frauenhaus.mitglied m
     JOIN frauenhaus.spende s ON m.mitglied = s.mitglied
      ↓
3. Word-Felder ersetzen:
   {Name} → "Mueller"
   {Betrag} → "100,00 Euro"
   {Datum} → "15.06.2024"
      ↓
4. Als PDF/Word speichern nach vorlagen/
      ↓
5. Outlook-Integration (falls verfügbar):
   - Autoversand per Email
```

---

## 🎯 Zusammenfassung – Was macht die App?

| Funktion | Beschreibung | DB-Tabelle |
|----------|-------------|-----------|
| **Benutzer-Verwaltung** | Anmeldung, Berechtigungen | compucrash.user_def |
| **Mitgliederverwaltung** | Adressen, Kontakte, Stichworte | frauenhaus.mitglied, stichwort |
| **Spendenverwaltung** | Erfassung, Quittungen, Reports | frauenhaus.spende, spendenart |
| **Bußgeldverwaltung** | Verwaltung, Zahlungseingang | frauenhaus.bussgeld, eingang |
| **Dokumenterstellung** | Briefe, Etiketten, Quittungen | Word-Vorlagen (.dot) |
| **Berichtswesen** | Excel-Reports für Geschäftsjahr | Reports / SpendenUebersicht |
| **Datensuche** | Volltextsuche, Filter | Stichwort-Zuordnungen |

---

## 🔧 Technische Details

**Verwendetes Design Pattern:**
- **MVC** (Model-View-Controller): Swing-GUI trennt von DB
- **DAO Pattern** (Data Access Object): CListDataManaging* / CInfoDataManaging*
- **Singleton Pattern**: CDataManager, CPropertyManager
- **Factory Pattern**: CDataObjectFactory, CButtonFactory

**Datenbankzugriff:**
- **Direktes JDBC** (keine JPA, kein ORM)
- **Raw SQL-Queries** (anfällig für SQL-Injection!)
- **Connection-pooling**: Standard-JDBC Einzelverbindung

**GUI-Framework:**
- **Java Swing** (veraltet, aber stabil)
- **JTable** für Listen
- **JFrame / JPanel** zur Fenster-Verwaltung
- **JComboBox** für Dropdowns
- **JButton** für Interaktion

---

## 📝 Zusammenfassung für Anfänger

**Einfach gesagt:**

1. **Starten:** Benutzer double-klicked `run_local.bat`
2. **Anmelsen:** Benutzer gibt Benutzername/Passwort ein
3. **Arbeiten:** Benutzer navigiert zu Datenbereich (z.B. "Mitglieder")
4. **Bearbeiten:** Benutzer klickt [Neu] / [Bearbeiten] / [Löschen]
5. **Berichte:** Benutzer erstellt Reports (z.B. "Spendenquittungen")
6. **Speichern:** Alles wird in SQL Server gespeichert
7. **Beenden:** Benutzer schließt das Fenster

Diese alte Anwendung ist bewährt, aber **dringend überholungsbedürftig** – darum haben wir die neue 3-Schichten Architektur entworfen (siehe `ARCHITEKTUR_NEU.md`). 🏗️
