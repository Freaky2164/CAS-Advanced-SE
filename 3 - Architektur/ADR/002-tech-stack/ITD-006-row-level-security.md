# Row Level Security und eingeschränkte DB-Rolle

## Das Problem
Personenbezogene Daten des Frauenhaus-Kontexts (DSGVO Art. 5/9, besondere Schutzpflicht) sind für jeden mit DB-Zugangsdaten (dorle:dorle) vollständig lesbar.

## Berücksichtigte Optionen

- Zugriffsschutz allein in der Anwendungsschicht (Spring Security)
- Spaltenverschlüsselung besonders sensibler Felder (pgcrypto)
- **Row Level Security auf allen Tabellen mit Personenbezug + Least-Privilege-Login-Rolle**

## Begründung
Scheint der leichteste Weg zum Schutz sensibler Daten zu sein und entspricht Best-Practices.

## Anmerkungen

