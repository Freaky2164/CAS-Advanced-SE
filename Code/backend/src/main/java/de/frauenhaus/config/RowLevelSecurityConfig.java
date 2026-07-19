package de.frauenhaus.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Umhüllt die automatisch konfigurierte DataSource mit einer
 * {@link RowLevelSecurityDataSource}, damit jede Datenbankverbindung den
 * Benutzerkontext für die Row-Level-Security-Policies trägt.
 *
 * @author Ole
 */
@Configuration(proxyBeanMethods = false)
public class RowLevelSecurityConfig {

    /** Verhindert Instanziierung von außen; Spring erzeugt die Konfiguration per Reflection. */
    private RowLevelSecurityConfig() { }

    /**
     * Registriert einen BeanPostProcessor, der DataSource-Beans in eine
     * {@link RowLevelSecurityDataSource} einpackt, ohne die
     * Auto-Konfiguration von Spring Boot zu ersetzen.
     *
     * @return der BeanPostProcessor für das Umhüllen der DataSource
     */
    @Bean
    static BeanPostProcessor rowLevelSecurityDataSourceWrapper() {
        return new BeanPostProcessor() {
            /**
             * Umhüllt alle DataSource-Beans, die noch keinen RLS-Wrapper haben.
             */
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof RowLevelSecurityDataSource)) {
                    return new RowLevelSecurityDataSource(dataSource);
                }
                return bean;
            }
        };
    }
}
