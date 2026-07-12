# 🔴 SQL Server Verbindungsfehler

## Problem
```
java.sql.SQLException: Unable to get information from SQL Server: localhost.
jdbc:jtds:sqlserver://localhost:1433/frauenhaus;instance=MSSQLSERVER
```

## ❌ Das bedeutet:

Die Anwendung **kann sich nicht mit SQL Server verbinden**. Ursachen:
1. SQL Server läuft nicht
2. SQL Server Instance heißt anders (z.B. `SQLEXPRESS` statt `MSSQLSERVER`)
3. Datenbank existiert nicht
4. Netzwerk-Probleme

---

## ✅ Schritt-für-Schritt Lösung

### **Schritt 1: SQL Server Diagnose durchführen**

**Öffne Command Prompt und führe aus:**
```cmd
cd "C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z"
diagnose_sqlserver.bat
```

Dies überprüft:
- ✓ Läuft SQL Server?
- ✓ Welche Instanz-Namen existieren?
- ✓ Kann man sich verbinden?
- ✓ Existiert die Datenbank?

**Notiere die Ausgabe, besonders:**
- Welche Instanzen angezeigt werden
- Bei welcher Verbindung `[SUCCESS]` erscheint

---

### **Schritt 2: SQL Server starten (Falls nicht laufen)**

Falls `diagnose_sqlserver.bat` keine erfolgreiche Verbindung anzeigt:

**Öffne Services:**
```cmd
services.msc
```

**Suche nach SQL Server:**
- `SQL Server (SQLEXPRESS)` – bei Express Edition
- `SQL Server (MSSQLSERVER)` – bei Standard Edition
- Oder andere Namen wie `SQLDEV`, `SQL2019`, etc.

**Starten:**
- Rechtsklick auf den Service → **"Starten"**
- Warte bis Status `Ausführed` wird

**Überprüfung:**
```cmd
tasklist | find /I "sqlservr.exe"
```

Sollte zeigen: `sqlservr.exe`

---

### **Schritt 3: localhost.ini anpassen**

Basierend auf Diagnose-Output, bearbeite `localhost.ini`:

**Finde diese Zeilen:**
```ini
dbhost=localhost
dbport=1433
dbsid=frauenhaus;instance\=MSSQLSERVER
```

**Mögliche Konfigurationen:**

#### **Option A: SQL Server Standard (Default)**
```ini
dbhost=localhost
dbport=1433
dbsid=frauenhaus
```

#### **Option B: SQL Server Express**
```ini
dbhost=localhost
dbport=1433
dbsid=frauenhaus;instance\=sqlexpress
```

#### **Option C: Andere Named Instance (z.B. sql2019)**
```ini
dbhost=localhost
dbport=1433
dbsid=frauenhaus;instance\=sql2019
```

#### **Option D: Netzwerk-Computer**
```ini
dbhost=SERVER-NAME
dbport=1433
dbsid=frauenhaus
```

---

### **Schritt 4: App erneut starten**

```cmd
cd "C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z"
run_local.bat
```

**Erfolg**, wenn:
- ✓ Keine Fehlermeldung
- ✓ Login-Fenster erscheint
- ✓ Login möglich

---

## 🔍 Debugging: Welche Instanz habe ich?

**Öffne Command Prompt:**
```cmd
REM Zeige alle verfuegbaren SQL Server Instanzen
sqlcmd -L
```

**Ausgabe-Beispiele:**

```
DESKTOP-ABC
DESKTOP-ABC\SQLEXPRESS
DESKTOP-ABC\MSSQLSERVER
```

Im Fehlerfall: Nutze den vollen Namen als `dbhost` in `localhost.ini`:
```ini
dbhost=DESKTOP-ABC\SQLEXPRESS
```

---

## ❓ Häufige Fehler

| Fehler-Symptom | Ursache | Lösung |
|---|---|---|
| `Unable to get information from SQL Server` | SQL Server läuft nicht | `services.msc` → Service starten |
| `Connection refused` | Falscher Port oder Service nicht gehört | PORT korrekt? (Standard: 1433) |
| `instance=MSSQLSERVER` aber sollte `SQLEXPRESS` sein | Falsche Instance-Name | Diagnose-Skript ausführen, korrekte Instance rausfinden |
| `Unable to connect to SQL Server` | Firewall blockiert | Windows Firewall: SQL Server erlauben |
| `Database 'frauenhaus' does not exist` | Backup noch nicht eingespielt | `Restore-Database.ps1` ausführen |

---

## 🎯 Kurzfassung

```
1. diagnose_sqlserver.bat ausführen
        ↓
2. SUCCESS-Meldung für Instanz notieren (z.B. "localhost\SQLEXPRESS")
        ↓
3. localhost.ini anpassen (dbsid=... Zeile korrigieren)
        ↓
4. run_local.bat ausführen
        ↓
5. Login-Fenster sollte erscheinen ✓
```

---

## 📞 Support

Falls **immer noch Fehler**:

1. Öffne Command Prompt als **Administrator**
   - Rechtsklick auf CMD → "Als Administrator ausführen"

2. Führe aus:
   ```cmd
   sqlcmd -S localhost -E -Q "SELECT @@VERSION"
   ```
   
   - Wenn erfolgreich → Instance ist erreichbar
   - Wenn Fehler → SQL Server-Problem

3. Überprüfe Firewall:
   ```cmd
   netsh advfirewall firewall show rule name="SQL Server" verbose
   ```

4. Falls Nothing hilft → SQL Server wahrscheinlich nicht installiert
   - [Download SQL Server Express (kostenfrei)](https://www.microsoft.com/en-us/sql-server/sql-server-downloads)

---

**Probiere zuerst `diagnose_sqlserver.bat` aus – das zeigt genau, was das Problem ist!** 🔧
