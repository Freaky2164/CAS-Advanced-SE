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
 * @author Nils
 *
 * Sicherheitskonfiguration mit zwei Filterketten: die REST-API bleibt zustandslos
 * per HTTP Basic abgesichert (z.B. für Skripte und Tests), das Vaadin-UI nutzt
 * eine Session mit Formular-Login über die {@link LoginView}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * @author Nils
     *
     * BCrypt-Passwort-Encoder für die in {@link de.frauenhaus.security.AppUser} gespeicherten Hashes.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * @author Nils
     *
     * Filterkette der REST-API: CSRF-Schutz deaktiviert (rein zustandslose API),
     * Health-Endpunkt frei zugänglich, Admin-Endpunkte nur für Rolle ADMIN,
     * alle übrigen Anfragen erfordern eine Anmeldung per HTTP Basic.
     */
    @Bean
    @Order(1)
    SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**", "/actuator/**")
                .csrf(csrf -> csrf.disable()) // zustandslose REST-API; bei Cookie-Sessions wieder aktivieren!
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(apiEntryPoint()));
        return http.build();
    }

    /**
     * @author Nils
     *
     * Filterkette des Vaadin-UIs: der {@link VaadinSecurityConfigurer} übernimmt
     * CSRF-Behandlung, statische Ressourcen und die annotierte Zugriffskontrolle
     * der Views (@PermitAll/@RolesAllowed); nicht angemeldete Benutzer landen
     * auf der Login-View.
     */
    @Bean
    @Order(2)
    SecurityFilterChain uiFilterChain(HttpSecurity http) throws Exception {
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class));
        return http.build();
    }

    /**
     * @author Nils
     *
     * 401 ohne "WWW-Authenticate: Basic"-Header, damit API-Clients im Browser
     * keinen nativen Login-Dialog auslösen.
     */
    private static AuthenticationEntryPoint apiEntryPoint() {
        return (request, response, authException) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}
