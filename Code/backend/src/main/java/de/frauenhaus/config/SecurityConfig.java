package de.frauenhaus.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * Sicherheitskonfiguration der REST-API: zustandslose HTTP-Basic-Authentifizierung
 * mit rollenbasiertem Zugriff (ADMIN vs. übrige Endpunkte).
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
     * Definiert die Filterkette: CSRF-Schutz deaktiviert (rein zustandslose API),
     * Health-Endpunkt frei zugänglich, Admin-Endpunkte nur für Rolle ADMIN,
     * alle übrigen Anfragen erfordern eine Anmeldung per HTTP Basic.
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // zustandslose REST-API; bei Cookie-Sessions wieder aktivieren!
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(spaEntryPoint()));
        return http.build();
    }

    /**
     * @author Nils
     *
     * 401 ohne "WWW-Authenticate: Basic"-Header, damit der Browser beim
     * SPA-Frontend keinen nativen Login-Dialog öffnet.
     */
    private static AuthenticationEntryPoint spaEntryPoint() {
        return (request, response, authException) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}
