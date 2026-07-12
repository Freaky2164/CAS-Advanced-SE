# 🚀 SCHNELLSTART – Frauenhaus Verwaltung (5 Minuten)

## Voraussetzungen
- [x] Windows 10/11
- [x] Java 17+ installiert (prüfe: `java -version`)
- [x] SQL Server 2008+ läuft
- [x] PowerShell Administrator-Zugriff

---

## 🔴 Schritt 1: Datenbank wiederherstellen (2 Min)

**PowerShell als Administrator öffnen** und ausführen:

```powershell
cd "C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z"
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
.\Restore-Database.ps1
```

**Erwartet**: Meldung `[SUCCESS] Datenbank erfolgreich wiederhergestellt!`

---

## 🟢 Schritt 2: Anwendung starten (1 Min)

**Command Prompt öffnen** und ausführen:

```cmd
cd "C:\Users\p.faller\Documents\Master\ASE\FH_MA\Z"
run_local.bat
```

**Erwartet**: 
1. Splash Screen (5 Sekunden)
2. Login-Fenster

---

## 🟡 Schritt 3: Anmelden

**Login-Fenster:**
- **Benutzername**: `dorle` (oder ein anderer aus der Datenbank)
- **Passwort**: ??? (ermittel aus Datenbank, siehe unten)

Falls du das Passwort nicht kennst, öffne SQL Server Management Studio:

```sql
USE frauenhaus;
SELECT user_name, password FROM compucrash.user_def;
```

---

## ❓ Häufige Probleme

### `java: command not found`
→ Java nicht im PATH. Installiere [Java 17 LTS](https://adoptium.net/download/)

### `Login failed for user 'sa'`
→ SQL Server läuft nicht oder nicht erreichbar  
Überprüfe: Services → SQL Server (Instanzname)

### `The index 'IX_...' does not exist`
→ Datenbank-Schema unvollständig  
Führe Restore-Database.ps1 erneut aus

### `FileNotFoundException: frauenhaus\vorlagen`
→ Verzeichnis fehlt – wird aber von run_local.bat erstellt!  
Nochmal run_local.bat ausführen

---

## 📋 Checkliste

- [ ] Java: `java -version` → mindestens Java 1.4
- [ ] SQL Server läuft: Prüfe Windows Services
- [ ] Restore-Status: grüne `[SUCCESS]`-Meldung sichtbar?
- [ ] Login: Benutzername und Passwort bekannt?
- [ ] GUI: Login-Fenster sichtbar?

---

## 💡 Tipps

| Anliegen | Was tun |
|----------|---------|
| **Logs anschauen** | Debug-Modus einschalten: `debug=true` in `localhost.ini` |
| **Konfiguration ändern** | `localhost.ini` bearbeiten (mit Notepad++, nicht MS Word!) |
| **Report-Speicherort** | In localhost.ini: `reports=` anpassen |
| **SQL Server Express verwenden** | In localhost.ini: `dbsid=frauenhaus;instance\=sqlexpress` |
| **Anderer PC / Netzwerk-DB** | Bearbeite: `dbhost=` und `dbport=` in localhost.ini |

---

## 🎓 Erste Schritte in der App

1. **Mitglied hinzufügen**: Hauptfenster → "Mitglieder" → "Neu"
2. **Spende erfassen**: "Spenden" → "Neu"
3. **Bericht erstellen**: "Reports" → Report-Typ wählen
4. **Daten exportieren**: Reports werden als .xlsx gespeichert in `C:\frauenhaus\reports\`

---

## 🆘 Support

Falls noch Fehler auftreten:
1. Überprüfe die Logs: Console-Ausgabe bei Start
2. Setze `debug=true` in `localhost.ini`
3. Prüfe die Datenbankverbindung: `SELECT @@version` in SQL Server
4. Siehe auch: `STARTUP_ANLEITUNG.md` (detaillierte Doku)

---

**Viel Erfolg! 🎉**
