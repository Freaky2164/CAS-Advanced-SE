# ⚡ SQL Server Schnell-Fixe (5 Minuten)

## 🔴 Dein Fehler
```
Unable to get information from SQL Server: localhost
```

**Das bedeutet:** SQL Server läuft nicht oder die Konfiguration stimmt nicht.

---

## ✅ Schnell-Lösung

### **1️⃣ Diagnose (2 Min)**

```cmd
cd C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z
diagnose_sqlserver.bat
```

**Schau auf die grünen `[SUCCESS]`-Meldungen:**

Wenn du siehst:
```
[SUCCESS] Verbindung zu localhost\SQLEXPRESS erfolgreich!
```

Dann notier dir: **`localhost\SQLEXPRESS`**

---

### **2️⃣ Konfiguration anpassen (2 Min)**

Öffne `localhost.ini` mit Notepad:

**Finde Zeile 13:**
```ini
dbsid=frauenhaus;instance\=MSSQLSERVER
```

**Ersetze mit (basierend auf Diagnose):**

- Wenn du `SQLEXPRESS` sahst:
  ```ini
  dbsid=frauenhaus;instance\=sqlexpress
  ```

- Wenn du `localhost` (ohne `\`) sahst:
  ```ini
  dbsid=frauenhaus
  ```

- Wenn du anderes sahst (z.B. `sql2019`):
  ```ini
  dbsid=frauenhaus;instance\=sql2019
  ```

**Speichern & Schließen**

---

### **3️⃣ App starten (1 Min)**

```cmd
run_local.bat
```

✨ **Login-Fenster sollte jetzt erscheinen!**

---

## 🆘 Falls immer noch Fehler

**Option A: SQL Server ist nicht installiert**

SQL Server kostenlos runterladen:
https://www.microsoft.com/en-us/sql-server/sql-server-downloads
- Wähle "Express" (kostenlos)
- Installiere
- Starte Service
- Nochmal `diagnose_sqlserver.bat` ausführen

**Option B: SQL Server läuft nicht**

```cmd
REM Öffne Services
services.msc
```

Suche: `SQL Server` oder `SQLEXPRESS`
→ Rechtsklick → **"Starten"**

**Option C: Immer noch nicht:**

Schau die ausführliche Dokumentation an:
- `FEHLER_SQLSERVER_VERBINDUNG.md` (detailliert)

---

**Das war's!** 🎉 Sollte jetzt funktionieren!
