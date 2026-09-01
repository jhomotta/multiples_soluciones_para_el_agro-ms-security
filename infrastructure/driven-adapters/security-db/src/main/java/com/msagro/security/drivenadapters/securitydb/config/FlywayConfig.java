package com.msagro.security.drivenadapters.securitydb.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Runs the Flyway migrations at startup over a plain JDBC connection.
 *
 * <p>The service talks to PostgreSQL reactively through R2DBC, but R2DBC has no migration
 * support, so Flyway opens its own short-lived JDBC connection from the {@code app.flyway.*}
 * properties. Spring Boot's own Flyway auto-configuration is switched off
 * ({@code spring.flyway.enabled=false}) so this bean is the single migration runner, and
 * {@code initMethod = "migrate"} makes it run before the first request is served.</p>
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(@Value("${app.flyway.url}") String url,
                         @Value("${app.flyway.user}") String user,
                         @Value("${app.flyway.password}") String password,
                         @Value("${app.flyway.locations:classpath:db/migration}") String locations,
                         @Value("${app.flyway.baseline-on-migrate:false}") boolean baselineOnMigrate) {
        return Flyway.configure()
                .dataSource(url, user, password)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .load();
    }
}
