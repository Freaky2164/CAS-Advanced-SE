-- Realistische Testdaten für lokale Entwicklung/Demo (nur Profil "dev", siehe application-dev.yml).
-- Reihenfolge folgt ops/DATENUEBERNAHME.md: anrede, verein, bussgeldstatus, spendentyp, gericht, stichwort
-- -> mitglied, spendenart -> verein_mitglied, stichwort_person, spende, bussgeld -> eingang.
-- 'Frauenhaus'/'Förderverein' kommen bereits aus V1__baseline_schema.sql und werden hier NICHT erneut
-- angelegt (sonst Verletzung des Primary Keys). Alle Namen/Adressen sind frei erfunden.

INSERT INTO frauenhaus.anrede (anrede) VALUES
    ('Frau'), ('Herr'), ('Familie'), ('Firma');

INSERT INTO frauenhaus.bussgeldstatus (bussgeldstatus) VALUES
    ('offen'), ('teilweise bezahlt'), ('bezahlt');

INSERT INTO frauenhaus.spendentyp (spendentyp) VALUES
    ('Geldspende dauer'), ('Mitgliedsbeitrag'), ('Dauerspende'), ('Sachspende');

INSERT INTO frauenhaus.spendenart (spendenart, spendentyp) VALUES
    ('Jahresbeitrag',        'Mitgliedsbeitrag'),
    ('Geldspende einmalig',  'Geldspende dauer'),
    ('Patenschaft monatlich','Dauerspende'),
    ('Kleiderspende',        'Sachspende'),
    ('Spielzeugspende',      'Sachspende'),
    ('Haushaltsspende',      'Sachspende');

INSERT INTO frauenhaus.gericht (bezeichnung, strasse, plz, ort) VALUES
    ('Amtsgericht München',           'Maximilianstraße 12',     '80539', 'München'),
    ('Amtsgericht Berlin-Mitte',      'Littenstraße 12-17',      '10179', 'Berlin'),
    ('Amtsgericht Hamburg-Altona',    'Max-Brauer-Allee 91',     '22765', 'Hamburg'),
    ('Amtsgericht Köln',              'Luxemburger Straße 101',  '50939', 'Köln'),
    ('Amtsgericht Frankfurt am Main', 'Gerichtsstraße 2',        '60313', 'Frankfurt am Main');

INSERT INTO frauenhaus.stichwort (stichwort) VALUES
    ('Newsletter'), ('Weihnachtsbrief'), ('Spendenaufruf'), ('Ehrenamt'), ('Patenschaft');

-- Mitglieder/Adressen (Spenderinnen, Vereinsmitglieder, Ehrenamtliche).
INSERT INTO frauenhaus.mitglied
    (anrede, vorname, name, briefanrede, strasse, plz, ort, email, tel1, foerderverein, frauenhaus, bemerkung) VALUES
    ('Frau',  'Sabine',  'Bergmann',  'Liebe Frau Bergmann',  'Ahornweg 4',        '70173', 'Stuttgart', 'sabine.bergmann@beispiel.de', '0711 1234560', true,  false, 'Langjähriges Fördervereinsmitglied'),
    ('Herr',  'Thomas',  'Reinhardt', 'Lieber Herr Reinhardt','Birkenstraße 17',   '80331', 'München',   'thomas.reinhardt@beispiel.de','089 1234561',  true,  false, NULL),
    ('Frau',  'Petra',   'König',     'Liebe Frau König',     'Lindenallee 9',     '50667', 'Köln',      'petra.koenig@beispiel.de',    '0221 1234562', false, true,  'Ehrenamtliche im Frauenhaus'),
    ('Frau',  'Anna',    'Vogel',     'Liebe Frau Vogel',      'Rosenweg 22',       '20095', 'Hamburg',   'anna.vogel@beispiel.de',      '040 1234563',  true,  true,  NULL),
    ('Herr',  'Michael', 'Schuster',  'Lieber Herr Schuster',  'Kastanienallee 3',  '10115', 'Berlin',    'michael.schuster@beispiel.de','030 1234564',  true,  false, NULL),
    ('Frau',  'Julia',   'Neumann',   'Liebe Frau Neumann',    'Bachstraße 11',     '60313', 'Frankfurt am Main', 'julia.neumann@beispiel.de', '069 1234565', false, false, 'Interessentin, noch kein Mitglied'),
    ('Herr',  'Stefan',  'Hoffmann',  'Lieber Herr Hoffmann',  'Talstraße 5',       '04109', 'Leipzig',   'stefan.hoffmann@beispiel.de', '0341 1234566', true,  false, NULL),
    ('Frau',  'Claudia', 'Fischer',   'Liebe Frau Fischer',    'Gartenweg 8',       '90402', 'Nürnberg',  'claudia.fischer@beispiel.de', '0911 1234567', true,  true,  NULL),
    ('Familie','',       'Weber',     'Liebe Familie Weber',   'Hauptstraße 30',    '01067', 'Dresden',   'familie.weber@beispiel.de',   '0351 1234568', true,  false, 'Spendet regelmäßig zu Weihnachten'),
    ('Herr',  'Andreas', 'Klein',     'Lieber Herr Klein',     'Feldweg 14',        '28195', 'Bremen',    'andreas.klein@beispiel.de',   '0421 1234569', false, false, NULL),
    ('Frau',  'Nicole',  'Wagner',    'Liebe Frau Wagner',     'Wiesenstraße 6',    '30159', 'Hannover',  'nicole.wagner@beispiel.de',   '0511 1234570', true,  false, NULL),
    ('Herr',  'Peter',   'Braun',     'Lieber Herr Braun',     'Bergstraße 19',     '40213', 'Düsseldorf','peter.braun@beispiel.de',     '0211 1234571', true,  false, NULL),
    ('Frau',  'Sandra',  'Lorenz',    'Liebe Frau Lorenz',     'Mühlenweg 2',       '55116', 'Mainz',     'sandra.lorenz@beispiel.de',   '06131 1234572',false, true,  'Ehrenamtliche im Frauenhaus'),
    ('Firma', '',        'Musterbau GmbH', 'Sehr geehrte Damen und Herren', 'Industriestraße 44', '70565', 'Stuttgart', 'spende@musterbau-beispiel.de', '0711 1234573', true, false, 'Firmenspende jährlich'),
    ('Frau',  'Karin',   'Schwarz',   'Liebe Frau Schwarz',    'Amselweg 7',        '86150', 'Augsburg',  'karin.schwarz@beispiel.de',   '0821 1234574', true,  false, NULL);

-- Verein-Zuordnung passend zu den foerderverein/frauenhaus-Flags oben.
INSERT INTO frauenhaus.verein_mitglied (verein, mitglied)
SELECT 'Förderverein', mitglied FROM frauenhaus.mitglied WHERE foerderverein = true
UNION ALL
SELECT 'Frauenhaus', mitglied FROM frauenhaus.mitglied WHERE frauenhaus = true;

-- Verteiler-Zuordnung (Stichworte).
INSERT INTO frauenhaus.stichwort_person (stichwort, mitglied)
SELECT 'Newsletter', mitglied FROM frauenhaus.mitglied WHERE name IN ('Bergmann', 'Reinhardt', 'Vogel', 'Hoffmann', 'Fischer', 'Wagner', 'Braun', 'Schwarz')
UNION ALL
SELECT 'Weihnachtsbrief', mitglied FROM frauenhaus.mitglied WHERE name IN ('Bergmann', 'Weber', 'Musterbau GmbH', 'Klein')
UNION ALL
SELECT 'Spendenaufruf', mitglied FROM frauenhaus.mitglied WHERE name IN ('Schuster', 'Neumann', 'Klein', 'Lorenz')
UNION ALL
SELECT 'Ehrenamt', mitglied FROM frauenhaus.mitglied WHERE name IN ('König', 'Lorenz');

-- Spenden der letzten beiden Jahre.
INSERT INTO frauenhaus.spende (mitglied, spendenart, verein, datum, betrag, bemerkung)
SELECT mitglied, 'Jahresbeitrag', 'Förderverein', d.datum, d.betrag, NULL
FROM frauenhaus.mitglied m
JOIN (VALUES
    ('Bergmann',  DATE '2025-01-15', 60.00),
    ('Reinhardt', DATE '2025-01-20', 60.00),
    ('Vogel',     DATE '2025-02-01', 60.00),
    ('Schuster',  DATE '2025-01-18', 60.00),
    ('Fischer',   DATE '2025-02-10', 60.00),
    ('Wagner',    DATE '2025-01-22', 60.00),
    ('Braun',     DATE '2024-01-14', 60.00),
    ('Schwarz',   DATE '2025-01-30', 60.00)
) AS d(name, datum, betrag) ON d.name = m.name;

INSERT INTO frauenhaus.spende (mitglied, spendenart, verein, datum, betrag, bemerkung)
SELECT mitglied, 'Geldspende einmalig', 'Frauenhaus', d.datum, d.betrag, d.bemerkung
FROM frauenhaus.mitglied m
JOIN (VALUES
    ('Bergmann',       DATE '2025-03-05', 100.00, NULL),
    ('Weber',          DATE '2024-12-18', 250.00, 'Weihnachtsspende'),
    ('Musterbau GmbH', DATE '2025-06-30', 1500.00, 'Firmenspende Halbjahr'),
    ('Neumann',        DATE '2025-04-12', 50.00,  NULL),
    ('Klein',          DATE '2025-05-01', 75.00,  NULL),
    ('Vogel',          DATE '2024-11-20', 200.00, 'Spendenaufruf Herbst')
) AS d(name, datum, betrag, bemerkung) ON d.name = m.name;

INSERT INTO frauenhaus.spende (mitglied, spendenart, verein, datum, betrag, bemerkung)
SELECT mitglied, 'Patenschaft monatlich', 'Frauenhaus', d.datum, d.betrag, 'Dauerauftrag'
FROM frauenhaus.mitglied m
JOIN (VALUES
    ('Fischer', DATE '2025-01-01', 25.00),
    ('Lorenz',  DATE '2025-01-01', 30.00),
    ('Schwarz', DATE '2025-02-01', 20.00)
) AS d(name, datum, betrag) ON d.name = m.name;

INSERT INTO frauenhaus.spende (mitglied, spendenart, verein, datum, betrag, bemerkung)
SELECT mitglied, 'Kleiderspende', 'Frauenhaus', d.datum, d.betrag, d.bemerkung
FROM frauenhaus.mitglied m
JOIN (VALUES
    ('König',    DATE '2025-03-15', 0.00, 'Sachspende: Kinderkleidung, geschätzter Wert 150 EUR'),
    ('Wagner',   DATE '2025-04-02', 0.00, 'Sachspende: Winterjacken')
) AS d(name, datum, betrag, bemerkung) ON d.name = m.name;

-- Bußgelder mit Gerichten und Zahlungseingängen.
INSERT INTO frauenhaus.bussgeld (gericht, verein, status, name, vorname, aktenzeichen, datum, zieldatum, betrag, bezahlt, bemerkung)
SELECT g.gericht, b.verein, b.status, b.name, b.vorname, b.aktenzeichen, b.datum, b.zieldatum, b.betrag, b.bezahlt, b.bemerkung
FROM (VALUES
    ('Amtsgericht München',           'Frauenhaus',    'bezahlt',            'Krause',   'Jörg',    '512 Js 4021/25', DATE '2025-02-10', DATE '2025-03-10', 500.00,  true,  NULL),
    ('Amtsgericht Berlin-Mitte',      'Frauenhaus',    'teilweise bezahlt',  'Lehmann',  'Frank',   '243 Js 1187/25', DATE '2025-03-04', DATE '2025-04-04', 800.00,  false, 'Ratenzahlung vereinbart'),
    ('Amtsgericht Hamburg-Altona',    'Förderverein',  'offen',              'Peters',   'Dirk',    '87 Js 552/25',   DATE '2025-05-20', DATE '2025-06-20', 350.00,  false, NULL),
    ('Amtsgericht Köln',              'Frauenhaus',    'bezahlt',            'Ackermann','Ralf',    '910 Js 3390/24', DATE '2024-11-11', DATE '2024-12-11', 620.00,  true,  NULL),
    ('Amtsgericht Frankfurt am Main', 'Frauenhaus',    'offen',              'Winter',   'Bernd',   '145 Js 771/25',  DATE '2025-06-02', DATE '2025-07-02', 450.00,  false, NULL),
    ('Amtsgericht München',           'Förderverein',  'teilweise bezahlt',  'Kaiser',   'Uwe',     '512 Js 4198/25', DATE '2025-04-18', DATE '2025-05-18', 900.00,  false, 'Ratenzahlung vereinbart')
) AS b(gericht_bez, verein, status, name, vorname, aktenzeichen, datum, zieldatum, betrag, bezahlt, bemerkung)
JOIN frauenhaus.gericht g ON g.bezeichnung = b.gericht_bez;

INSERT INTO frauenhaus.eingang (bussgeld, datum, betrag, bemerkung)
SELECT bu.bussgeld, e.datum, e.betrag, e.bemerkung
FROM frauenhaus.bussgeld bu
JOIN (VALUES
    ('512 Js 4021/25', DATE '2025-03-01', 500.00, NULL),
    ('243 Js 1187/25', DATE '2025-03-20', 400.00, '1. Rate'),
    ('910 Js 3390/24', DATE '2024-12-05', 620.00, NULL),
    ('512 Js 4198/25', DATE '2025-05-01', 300.00, '1. Rate')
) AS e(aktenzeichen, datum, betrag, bemerkung) ON e.aktenzeichen = bu.aktenzeichen;
