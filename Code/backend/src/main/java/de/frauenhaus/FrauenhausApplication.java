package de.frauenhaus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Nils
 *
 * Einstiegspunkt der Spring-Boot-Anwendung, die das alte compucrash-System
 * (Verwaltung von Frauenhaus/Förderverein, Spenden, Mitgliedern und Bußgeldern) ablöst.
 */
@SpringBootApplication
public class FrauenhausApplication {

    /** Startet den Spring-Application-Context. */
    public static void main(String[] args) {
        SpringApplication.run(FrauenhausApplication.class, args);
    }
}
