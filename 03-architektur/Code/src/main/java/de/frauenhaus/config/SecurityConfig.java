package de.frauenhaus.config;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import de.frauenhaus.ui.LoginView;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sicherheitskonfiguration mit zwei Filterketten: die REST-API ist zustandslos
 * per HTTP Basic abgesichert, das Vaadin-UI nutzt eine Session mit
 * Formular-Login über die {@link LoginView}.
 *
 * @author Paul
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * BCrypt-Passwort-Encoder für die gespeicherten Benutzer-Hashes.
     *
     * @return der Passwort-Encoder
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Filterkette der REST-API: Health-Endpunkt frei zugänglich, Admin-Endpunkte
     * nur für die Rolle ADMIN, alle übrigen Anfragen erfordern eine Anmeldung
     * per HTTP Basic. Der CSRF-Schutz ist hier deaktiviert, weil die API
     * zustandslos ist: es gibt keine Session-Cookies, jede Anfrage
     * authentifiziert sich selbst per HTTP Basic, sodass keine
     * Cross-Site-Request-Forgery-Angriffsfläche besteht.
     *
     * @param http der HttpSecurity-Builder
     * @return die gebaute Filterkette für {@code /api/**} und {@code /actuator/**}
     */
    @Bean
    @Order(1)
    SecurityFilterChain apiFilterChain(HttpSecurity http) {
        try {
            http
                    .securityMatcher("/api/**", "/actuator/**")
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/actuator/health/**").permitAll()
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated())
                    .httpBasic(basic -> basic.authenticationEntryPoint(apiEntryPoint()));
            return http.build();
        } catch (Exception e) {
            throw new IllegalStateException("API-Sicherheitskonfiguration konnte nicht gebaut werden", e);
        }
    }
    /**
     * Filterkette des Vaadin-UIs: der {@link VaadinSecurityConfigurer} übernimmt
     * CSRF-Behandlung, statische Ressourcen und die annotierte Zugriffskontrolle
     * der Views; nicht angemeldete Benutzer landen auf der Login-View.
     * {@code /error} ist freigegeben, damit API-Fehler als Statuscode statt als
     * Umleitung auf die Login-Seite beantwortet werden.
     *
     * @param http der HttpSecurity-Builder
     * @return die gebaute Filterkette für das UI
     */
    @Bean
    @Order(2)
    SecurityFilterChain uiFilterChain(HttpSecurity http) {
        try {
            http
                    .authorizeHttpRequests(auth -> auth.requestMatchers("/error").permitAll())
                    .with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class));
            return http.build();
        } catch (Exception e) {
            throw new IllegalStateException("UI-Sicherheitskonfiguration konnte nicht gebaut werden", e);
        }
    }

    /**
     * Liefert einen Entry-Point, der 401 ohne "WWW-Authenticate: Basic"-Header
     * sendet, damit Browser keinen nativen Login-Dialog anzeigen.
     *
     * @return der Entry-Point für die API-Filterkette
     */
    private static AuthenticationEntryPoint apiEntryPoint() {
        return (request, response, authException) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}
