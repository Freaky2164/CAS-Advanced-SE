package de.frauenhaus.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Nils
 *
 * Testet {@link BetragInWorten#von(BigDecimal)} für ganze Beträge, Centbeträge und den Nullfall.
 */
class BetragInWortenTest {

    @Test
    void ganzeBetraege() {
        assertEquals("*eins Euro*", BetragInWorten.von(new BigDecimal("1.00")));
        assertEquals("*einundzwanzig Euro*", BetragInWorten.von(new BigDecimal("21")));
        assertEquals("*einhundert Euro*", BetragInWorten.von(new BigDecimal("100")));
        assertEquals("*eintausendzweihundertvierunddreißig Euro*", BetragInWorten.von(new BigDecimal("1234")));
        assertEquals("*eintausend Euro*", BetragInWorten.von(new BigDecimal("1000")));
        assertEquals("*einhunderteintausend Euro*", BetragInWorten.von(new BigDecimal("101000")));
        assertEquals("*zwanzigtausendeins Euro*", BetragInWorten.von(new BigDecimal("20001")));
    }

    @Test
    void mitCent() {
        assertEquals("*fünfzig Euro und fünf Cent*", BetragInWorten.von(new BigDecimal("50.05")));
    }

    @Test
    void null_euro() {
        assertEquals("*null Euro*", BetragInWorten.von(BigDecimal.ZERO));
    }

    @Test
    void ohneWaehrungFuerVorlagenMitEuroImText() {
        assertEquals("*fünfzig*", BetragInWorten.ohneWaehrung(new BigDecimal("50.00")));
        assertEquals("*fünfzig Komma null fünf*", BetragInWorten.ohneWaehrung(new BigDecimal("50.05")));
        assertEquals("*fünf Komma elf*", BetragInWorten.ohneWaehrung(new BigDecimal("5.11")));
        assertEquals("*null*", BetragInWorten.ohneWaehrung(BigDecimal.ZERO));
    }
}
