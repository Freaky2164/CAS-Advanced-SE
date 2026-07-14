package de.frauenhaus.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author Nils
 *
 * Hängt {@link RowLevelSecurityDataSource} um die automatisch konfigurierte
 * (Hikari-)DataSource, damit jede Datenbankverbindung den Benutzerkontext für
 * die Row-Level-Security-Policies trägt.
 */
@Configuration
public class RowLevelSecurityConfig {

    /**
     * @author Nils
     *
     * BeanPostProcessor statt eigener DataSource-Definition, damit Spring Boots
     * Auto-Konfiguration (Pooling, Properties, Metriken) unverändert bleibt.
     */
    @Bean
    static BeanPostProcessor rowLevelSecurityDataSourceWrapper() {
        return new BeanPostProcessor() {
            /**
             * @author Nils
             *
             * Umhüllt genau die DataSource-Beans, die noch keinen RLS-Wrapper haben.
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
