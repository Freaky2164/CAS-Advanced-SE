-- @author Nils
-- Vorbereitung für das Einspielen des Altsystem-Backups (data.sql):
-- Der pg_dump stammt aus einer Konvertierung (rebasedata) und setzt die Rolle
-- "rebasedata" als Tabellen-Eigentümer voraus. Die Rolle wird nach der
-- Datenübernahme (04_datenuebernahme.sql) wieder entfernt.
CREATE ROLE rebasedata;
