package de.frauenhaus;

import com.vaadin.flow.component.page.AppShellConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Nils
 *
 * Einstiegspunkt der Spring-Boot-Anwendung, die das alte compucrash-System
 * (Verwaltung von Frauenhaus/Förderverein, Spenden, Mitgliedern und Bußgeldern) ablöst.
 * Dient zugleich als Vaadin-AppShell (Index-Seite des server-seitigen UIs).
 */
@SpringBootApplication
public class FrauenhausApplication implements AppShellConfigurator {

    /**
     * @author Nils
     *
     * Startet den Spring-Application-Context.
     */
    public static void main(String[] args) {
        SpringApplication.run(FrauenhausApplication.class, args);
    }
}
