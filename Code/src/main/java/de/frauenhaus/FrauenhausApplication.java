package de.frauenhaus;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Einstiegspunkt der Spring-Boot-Anwendung zur Verwaltung von Spenden,
 * Mitgliedern und Bußgeldern des Fördervereins. Dient zugleich als
 * Vaadin-AppShell und lädt das Aura-Theme.
 *
 * @author Paul
 */
@SpringBootApplication
@StyleSheet(Aura.STYLESHEET)
public class FrauenhausApplication implements AppShellConfigurator {

    /**
     * Startet den Spring-Application-Context.
     *
     * @param args Kommandozeilenargumente
     */
    public static void main(String[] args) {
        SpringApplication.run(FrauenhausApplication.class, args);
    }
}
