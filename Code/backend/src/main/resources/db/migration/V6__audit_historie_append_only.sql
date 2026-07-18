-- @author Nils
-- Härtung nach Gruppen-Review: Die App-Rolle frauenhaus_backend kann den
-- RLS-Benutzerkontext (app.benutzer/app.benutzer_rolle) selbst per set_config
-- setzen – RLS ist damit kein Schutz gegen geleakte Backend-Zugangsdaten.
-- Damit ein Angreifer mit diesen Zugangsdaten wenigstens die Änderungshistorie
-- nicht manipulieren oder löschen kann, wird die Audit-Historie append-only:
-- Envers schreibt nur INSERTs (DefaultAuditStrategy) und liest für den Verlauf –
-- UPDATE/DELETE braucht die Anwendung dort nie.
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'frauenhaus_backend') THEN
        REVOKE UPDATE, DELETE ON
            frauenhaus.mitglied_aud,
            frauenhaus.spende_aud,
            frauenhaus.bussgeld_aud,
            frauenhaus.verein_aud,
            frauenhaus.gericht_aud,
            app.revinfo
        FROM frauenhaus_backend;
    END IF;
END
$$;
