package de.frauenhaus.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Nils
 *     <p>Hängt {@link RowLevelSecurityDataSource} um die automatisch konfigurierte
 *     (Hikari-)DataSource, damit jede Datenbankverbindung den Benutzerkontext für die
 *     Row-Level-Security-Policies trägt.
 */
@Configuration
public class RowLevelSecurityConfig {

  /**
   * @author Nils
   *     <p>BeanPostProcessor statt eigener DataSource-Definition, damit Spring Boots
   *     Auto-Konfiguration (Pooling, Properties, Metriken) unverändert bleibt.
   */
  @Bean
  static BeanPostProcessor rowLevelSecurityDataSourceWrapper() {
    return new BeanPostProcessor() {
      /**
       * @author Nils
       *     <p>Umhüllt genau die DataSource-Beans, die noch keinen RLS-Wrapper haben.
       */
      @Override
      public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource dataSource
            && !(bean instanceof RowLevelSecurityDataSource)) {
          return new RowLevelSecurityDataSource(dataSource);
        }
        return bean;
      }
    };
  }
}
