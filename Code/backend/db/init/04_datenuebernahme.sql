-- @author Nils
-- Datenübernahme aus dem Altsystem-Backup (data.sql, eingespielt als 03_altdaten.sql).
--
-- Das Backup stammt aus einer rebasedata-Konvertierung: alle Fachtabellen liegen als
-- public._frauenhaus_* vor, sämtliche Spalten sind untypisiert (Datum/Betrag als varchar,
-- leere Strings statt NULL). Dieses Skript überführt die Daten in das Zielschema
-- frauenhaus.* (01_schema.sql) und räumt anschließend alle Alt-Tabellen wieder weg.
--
-- Bekannte Datenbereinigungen:
--   * leere Strings -> NULL
--   * 'YYYY-MM-DD HH24:MI:SS'-Strings -> date
--   * Bußgelder/Spenden ohne Betrag -> 0.00
--   * Spenden ohne Datum erhalten das Platzhalterdatum 1900-01-01, Spenden ohne
--     (zuordenbaren) Träger den Ersatz-Träger 'unbekannt'; beides wird in der
--     Bemerkung gekennzeichnet (Gruppen-Review: wegen Aufbewahrungsfristen darf
--     KEIN Spendeneintrag bei der Übernahme verworfen werden, s. NOTICE)
--   * Spendenart wird case-insensitiv der Lookup-Tabelle zugeordnet ('spende' -> 'Spende');
--     Spenden ohne zuordenbare Spendenart erhalten die Ersatz-Spendenart 'unbekannt'
--   * bussgeld.bezahlt (neue Spalte) wird aus dem Status abgeleitet ('bezahlt'/'abgeschlossen')

BEGIN;

-- Lookups ---------------------------------------------------------------

INSERT INTO frauenhaus.anrede (anrede)
SELECT DISTINCT anrede FROM public._frauenhaus_anrede
WHERE coalesce(anrede, '') <> ''
ON CONFLICT DO NOTHING;

-- Sicherheitsnetz: auch Anreden, die nur in mitglied vorkommen
INSERT INTO frauenhaus.anrede (anrede)
SELECT DISTINCT anrede FROM public._frauenhaus_mitglied
WHERE coalesce(anrede, '') <> ''
ON CONFLICT DO NOTHING;

-- 'Frauenhaus' und 'Förderverein' legt bereits 01_schema.sql an
INSERT INTO frauenhaus.verein (verein, bezeichnung)
SELECT verein, verein FROM public._frauenhaus_verein
WHERE coalesce(verein, '') <> ''
ON CONFLICT DO NOTHING;

INSERT INTO frauenhaus.bussgeldstatus (bussgeldstatus)
SELECT DISTINCT status FROM public._frauenhaus_bussgeldstatus
WHERE coalesce(status, '') <> ''
ON CONFLICT DO NOTHING;

INSERT INTO frauenhaus.spendentyp (spendentyp)
SELECT DISTINCT spendentyp FROM public._frauenhaus_spendentyp
WHERE coalesce(spendentyp, '') <> ''
ON CONFLICT DO NOTHING;

INSERT INTO frauenhaus.spendenart (spendenart, spendentyp)
SELECT spendenart, spendentyp FROM public._frauenhaus_spendenart
WHERE coalesce(spendenart, '') <> '' AND coalesce(spendentyp, '') <> ''
ON CONFLICT DO NOTHING;

-- Ersatz-Spendenart für Alt-Spenden ohne (zuordenbare) Spendenart
INSERT INTO frauenhaus.spendentyp (spendentyp) VALUES ('Geldspende einmalig')
ON CONFLICT DO NOTHING;
INSERT INTO frauenhaus.spendenart (spendenart, spendentyp) VALUES ('unbekannt', 'Geldspende einmalig')
ON CONFLICT DO NOTHING;

INSERT INTO frauenhaus.stichwort (stichwort)
SELECT DISTINCT stichwort FROM public._frauenhaus_stichwort
WHERE coalesce(stichwort, '') <> ''
ON CONFLICT DO NOTHING;

-- Sicherheitsnetz: Stichworte, die nur in der Zuordnungstabelle vorkommen
INSERT INTO frauenhaus.stichwort (stichwort)
SELECT DISTINCT stichwort FROM public._frauenhaus_stichwort_person
WHERE coalesce(stichwort, '') <> ''
ON CONFLICT DO NOTHING;

-- Stammdaten (IDs bleiben erhalten, Sequenzen werden unten nachgezogen) --

INSERT INTO frauenhaus.gericht (gericht, bezeichnung, strasse, plz, ort)
SELECT gericht,
       coalesce(nullif(bezeichnung, ''), 'unbekannt'),
       nullif(strasse, ''),
       nullif(plz, ''),
       nullif(ort, '')
FROM public._frauenhaus_gericht;

INSERT INTO frauenhaus.mitglied (mitglied, anrede, vorname, name, name2, name3, briefanrede,
                                 strasse, plz, ort, email, tel1, tel2, fax,
                                 foerderverein, frauenhaus, bemerkung)
SELECT mitglied,
       nullif(anrede, ''),
       nullif(vorname, ''),
       name,
       nullif(name2, ''),
       nullif(name3, ''),
       nullif(briefanrede, ''),
       nullif(strasse, ''),
       plz::text,
       nullif(ort, ''),
       nullif(email, ''),
       nullif(tel1, ''),
       nullif(tel2, ''),
       nullif(fax, ''),
       coalesce(foerderverein, '') = '1',
       coalesce(frauenhaus, '') = '1',
       nullif(bem, '')
FROM public._frauenhaus_mitglied;
-- Hinweis: mitglied.stichwort (zusammengeklebter Text) wird bewusst nicht übernommen –
-- die m:n-Zuordnung steckt vollständig in _frauenhaus_stichwort_person.

INSERT INTO frauenhaus.verein_mitglied (verein, mitglied)
SELECT DISTINCT verein, mitglied FROM public._frauenhaus_verein_mitglied
WHERE coalesce(verein, '') <> ''
ON CONFLICT DO NOTHING;

INSERT INTO frauenhaus.stichwort_person (stichwort, mitglied)
SELECT DISTINCT stichwort, mitglied FROM public._frauenhaus_stichwort_person
WHERE coalesce(stichwort, '') <> ''
ON CONFLICT DO NOTHING;

-- Bewegungsdaten ---------------------------------------------------------

INSERT INTO frauenhaus.bussgeld (bussgeld, gericht, verein, status, name, vorname,
                                 aktenzeichen, datum, zieldatum, betrag, bezahlt)
SELECT bussgeld,
       gericht,
       verein,
       nullif(status, ''),
       nullif(name, ''),
       nullif(vorname, ''),
       nullif(aktenzeichen, ''),
       left(datum, 10)::date,
       left(nullif(zieldatum, ''), 10)::date,
       coalesce(nullif(betrag, '')::numeric, 0),
       coalesce(status, '') IN ('bezahlt', 'abgeschlossen')
FROM public._frauenhaus_bussgeld;

INSERT INTO frauenhaus.eingang (eingang, bussgeld, datum, betrag)
SELECT eingang,
       bussgeld,
       left(datum, 10)::date,
       coalesce(betrag, 0)
FROM public._frauenhaus_eingang;

-- Ersatz-Träger für Alt-Spenden ohne (zuordenbaren) Träger – Spenden dürfen bei
-- der Übernahme nicht verworfen werden (Aufbewahrungsfristen, Gruppen-Review).
INSERT INTO frauenhaus.verein (verein, bezeichnung)
VALUES ('unbekannt', 'unbekannt (Platzhalter aus der Datenübernahme)')
ON CONFLICT (verein) DO NOTHING;

DO $$
DECLARE
    mit_platzhalter integer;
BEGIN
    SELECT count(*) INTO mit_platzhalter
    FROM public._frauenhaus_spende
    WHERE coalesce(datum, '') = '' OR coalesce(verein, '') = '';
    IF mit_platzhalter > 0 THEN
        RAISE NOTICE 'Datenübernahme: % Alt-Spenden ohne Datum oder Träger mit gekennzeichneten Platzhaltern übernommen (Datum 1900-01-01 bzw. Träger ''unbekannt'')', mit_platzhalter;
    END IF;
END $$;

INSERT INTO frauenhaus.spende (spende, mitglied, spendenart, verein, datum, betrag, bemerkung)
SELECT s.spende,
       s.mitglied,
       coalesce(a.spendenart, 'unbekannt'),
       coalesce(v.verein, 'unbekannt'),
       coalesce(nullif(left(s.datum, 10), '')::date, DATE '1900-01-01'),
       coalesce(nullif(s.betrag, '')::numeric, 0),
       nullif(concat_ws(' | ',
           nullif(s.bemerkung, ''),
           CASE WHEN coalesce(s.datum, '') = ''
                THEN 'Datenübernahme: Datum fehlte im Altsystem (Platzhalter 01.01.1900)' END,
           CASE WHEN v.verein IS NULL
                THEN 'Datenübernahme: Träger fehlte im Altsystem oder war nicht zuordenbar'
                     || CASE WHEN coalesce(s.verein, '') <> '' THEN ' (Altwert: ''' || s.verein || ''')' ELSE '' END
           END), '')
FROM public._frauenhaus_spende s
LEFT JOIN frauenhaus.spendenart a ON lower(a.spendenart) = lower(s.spendenart)
LEFT JOIN frauenhaus.verein v ON v.verein = s.verein;

-- Identity-Sequenzen hinter die übernommenen IDs setzen -------------------

SELECT setval(pg_get_serial_sequence('frauenhaus.gericht',  'gericht'),  coalesce(max(gericht),  0) + 1, false) FROM frauenhaus.gericht;
SELECT setval(pg_get_serial_sequence('frauenhaus.mitglied', 'mitglied'), coalesce(max(mitglied), 0) + 1, false) FROM frauenhaus.mitglied;
SELECT setval(pg_get_serial_sequence('frauenhaus.bussgeld', 'bussgeld'), coalesce(max(bussgeld), 0) + 1, false) FROM frauenhaus.bussgeld;
SELECT setval(pg_get_serial_sequence('frauenhaus.eingang',  'eingang'),  coalesce(max(eingang),  0) + 1, false) FROM frauenhaus.eingang;
SELECT setval(pg_get_serial_sequence('frauenhaus.spende',   'spende'),   coalesce(max(spende),   0) + 1, false) FROM frauenhaus.spende;

-- Kontrolle ins Init-Log
DO $$
BEGIN
    RAISE NOTICE 'Datenübernahme abgeschlossen: % Mitglieder, % Spenden, % Bußgelder, % Eingänge, % Stichwort-Zuordnungen',
        (SELECT count(*) FROM frauenhaus.mitglied),
        (SELECT count(*) FROM frauenhaus.spende),
        (SELECT count(*) FROM frauenhaus.bussgeld),
        (SELECT count(*) FROM frauenhaus.eingang),
        (SELECT count(*) FROM frauenhaus.stichwort_person);
END $$;

-- Aufräumen: alle Alt-Tabellen (public._*) und die Import-Rolle entfernen --

DO $$
DECLARE
    t text;
BEGIN
    FOR t IN SELECT tablename FROM pg_tables
             WHERE schemaname = 'public' AND tablename LIKE '\_%'
    LOOP
        EXECUTE format('DROP TABLE public.%I', t);
    END LOOP;
END $$;

COMMIT;

DROP ROLE rebasedata;
