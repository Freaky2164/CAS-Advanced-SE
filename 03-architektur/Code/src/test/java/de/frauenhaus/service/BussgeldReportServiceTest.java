package de.frauenhaus.service;

import de.frauenhaus.domain.Verein;
import de.frauenhaus.repository.BussgeldRepository;
import de.frauenhaus.repository.VereinRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests der Bußgeld-Übersicht. Wichtigster Punkt: die Träger kommen aus den
 * Stammdaten, damit auch der Platzhalter-Träger {@code unbekannt} aus der
 * Datenübernahme im Report auftaucht.
 *
 * @author Robin
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BussgeldReportServiceTest {

    private static final LocalDate VON = LocalDate.of(2025, 1, 1);
    private static final LocalDate BIS = LocalDate.of(2025, 12, 31);

    @Mock
    private BussgeldRepository bussgelder;

    @Mock
    private VereinRepository vereine;

    @InjectMocks
    private BussgeldReportService service;

    private static BussgeldRepository.UebersichtZeile zeile(String gericht, String bussgeld, String eingang)
    {
        return new BussgeldRepository.UebersichtZeile() {
            @Override
            public String getBezeichnung()
            {
                return gericht;
            }

            @Override
            public BigDecimal getBussgelder()
            {
                return new BigDecimal(bussgeld);
            }

            @Override
            public BigDecimal getEingaenge()
            {
                return new BigDecimal(eingang);
            }
        };
    }

    private static List<String> textzellen(byte[] xlsx) throws IOException
    {
        List<String> werte = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                row.forEach(zelle -> {
                    if (zelle.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                        werte.add(zelle.getStringCellValue());
                    }
                });
            }
        }
        return werte;
    }

    @Test
    @DisplayName("die Übersicht enthält alle Träger aus den Stammdaten")
    void uebersicht_alleTraeger_erscheinenImReport() throws IOException
    {
        when(vereine.findAll(any(Sort.class))).thenReturn(List.of(
                new Verein("Frauenhaus", "Frauenhaus"),
                new Verein("Förderverein", "Förderverein")));
        when(bussgelder.uebersicht(eq(VON), eq(BIS), any()))
                .thenReturn(List.of(zeile("Amtsgericht München", "500.00", "200.00")));

        List<String> zellen = textzellen(service.uebersicht(VON, BIS));

        assertThat(zellen).contains("Frauenhaus", "Förderverein", "Amtsgericht München");
    }

    @Test
    @DisplayName("der Platzhalter-Träger 'unbekannt' aus der Datenübernahme wird nicht unterschlagen")
    void uebersicht_platzhalterTraeger_erscheintImReport() throws IOException
    {
        when(vereine.findAll(any(Sort.class))).thenReturn(List.of(
                new Verein("Frauenhaus", "Frauenhaus"),
                new Verein("unbekannt", "unbekannt (Platzhalter aus der Datenübernahme)")));
        when(bussgelder.uebersicht(eq(VON), eq(BIS), any())).thenReturn(List.of());

        List<String> zellen = textzellen(service.uebersicht(VON, BIS));

        assertThat(zellen).contains("unbekannt", "Summe unbekannt");
        verify(bussgelder).uebersicht(VON, BIS, "unbekannt");
    }

    @Test
    @DisplayName("ohne Träger entsteht trotzdem eine gültige, leere Datei")
    void uebersicht_ohneTraeger_liefertLeereDatei()
    {
        when(vereine.findAll(any(Sort.class))).thenReturn(List.of());

        byte[] xlsx = service.uebersicht(VON, BIS);

        assertThat(xlsx).isNotEmpty();
        assertThat(new String(xlsx, 0, 2)).isEqualTo("PK");
    }

    @Test
    @DisplayName("die Summenzeile addiert die Zeilen des Trägers")
    void uebersicht_summenzeile_wirdGeschrieben() throws IOException
    {
        when(vereine.findAll(any(Sort.class))).thenReturn(List.of(new Verein("Frauenhaus", "Frauenhaus")));
        when(bussgelder.uebersicht(eq(VON), eq(BIS), eq("Frauenhaus"))).thenReturn(List.of(
                zeile("Amtsgericht München", "500.00", "200.00"),
                zeile("Amtsgericht Köln", "300.00", "100.00")));

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(service.uebersicht(VON, BIS)))) {
            Row summe = wb.getSheetAt(0).getRow(3);
            assertThat(summe.getCell(0).getStringCellValue()).isEqualTo("Summe Frauenhaus");
            assertThat(summe.getCell(1).getNumericCellValue()).isEqualTo(800.0);
            assertThat(summe.getCell(2).getNumericCellValue()).isEqualTo(300.0);
        }
    }
}
