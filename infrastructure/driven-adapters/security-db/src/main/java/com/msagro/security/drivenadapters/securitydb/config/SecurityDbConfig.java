package com.msagro.security.drivenadapters.securitydb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Enables the reactive repositories of this adapter and reactive transaction management, so the
 * {@code @Transactional} annotations on the use cases actually take effect.
 *
 * <p>The {@code ConnectionFactory}, {@code DatabaseClient} and {@code R2dbcEntityTemplate} come
 * from Spring Boot's auto-configuration, driven by the {@code spring.r2dbc.*} properties.</p>
 */
@Configuration
@EnableTransactionManagement
@EnableR2dbcRepositories(basePackages = "com.msagro.security.drivenadapters.securitydb.repository")
public class SecurityDbConfig {
}
