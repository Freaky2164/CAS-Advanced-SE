package de.frauenhaus;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Nils
 *
 * Einstiegspunkt der Spring-Boot-Anwendung, die das alte compucrash-System
 * (Verwaltung von Frauenhaus/Förderverein, Spenden, Mitgliedern und Bußgeldern) ablöst.
 * Dient zugleich als Vaadin-AppShell; das Aura-Theme muss seit Vaadin 25 explizit
 * geladen werden – ohne @StyleSheet gäbe es nur ungestylte Browser-Basisdarstellung.
 */
@SpringBootApplication
@StyleSheet(Aura.STYLESHEET)
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
