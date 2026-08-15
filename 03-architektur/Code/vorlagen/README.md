# Word-Vorlagen (.dot)

**Dieses Verzeichnis ist im Repository absichtlich leer.**

Die Word-Vorlagen für Bußgeldbestätigungen und Spendenbescheinigungen sind
fachliche Dokumente des Vereins. Sie werden von der **Vorlagen-Gruppe**
bereitgestellt und gepflegt und **nicht** in dieses Repository eingecheckt.

## Was hier abgelegt werden muss

| Datei | Zweck |
|---|---|
| `FHBG.dot` | Zahlungsbestätigung an das Gericht – Träger *Frauenhaus* |
| `FVBG.dot` | Zahlungsbestätigung an das Gericht – Träger *Förderverein* |
| `FHSB.dot` / `FVSB.dot` | allgemeine Spendenbescheinigung je Träger (Fallback) |
| `FHSBGeldspende.dot`, `FHSBDauerspende.dot`, `FHSBMitgliedsbeitrag.dot`, `FHSBSachspende.dot` | Spendenbescheinigung je Spendentyp – Träger *Frauenhaus* |
| `FVSBGeldspende.dot`, `FVSBDauerspende.dot`, `FVSBMitgliedsbeitrag.dot`, `FVSBSachspende.dot` | dieselben je Spendentyp – Träger *Förderverein* |

Fehlt eine typspezifische Vorlage, greift automatisch die allgemeine
`FHSB.dot` bzw. `FVSB.dot`.

## Format

Word-Binärformat (`.dot`, gelesen mit POI-HWPF). Die Werte werden über
**Lesezeichen** eingesetzt – die Lesezeichennamen müssen exakt so heißen:

- Bußgeldbestätigung: `bezeichnung`, `strasse`, `plz`, `ort`, `name`, `vorname`,
  `aktenzeichen`, `betrag`, `datum`, `datumbetrag`, `restsumme`
- Spendenbescheinigung: `bescheinigung`, `vorname`, `name`, `strasse`, `plz`,
  `ort`, `betrag`, `worte`, `datum`, `einzelbetrag`

Lesezeichen ohne passenden Wert bleiben unverändert stehen.
`datumbetrag` und `einzelbetrag` erhalten mehrzeilige Werte; die
Absatzformatierung des Lesezeichen-Absatzes (z.B. Aufzählungen) wird für jede
Zeile weitergeführt.

## Ohne Vorlagen

Die Anwendung startet normal. Nur die beiden Endpunkte
`/api/reports/bussgeld-bestaetigung/{id}` und `/api/reports/spendenquittung/{id}`
melden dann `Vorlage … nicht gefunden – app.vorlagen.pfad prüfen`.

Der Pfad ist über `APP_VORLAGEN_PFAD` konfigurierbar (Default `vorlagen`, im
Docker-Image `/app/vorlagen`).
