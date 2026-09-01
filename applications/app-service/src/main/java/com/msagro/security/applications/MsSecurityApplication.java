package com.msagro.security.applications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry point of ms-security — the only runnable module.
 *
 * <p>The component scan spans {@code com.msagro.security}, so the controllers, use cases,
 * adapters, mappers and configuration spread across the Clean Architecture modules are all
 * discovered here. Flyway runs before the first request is served.</p>
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.msagro.security")
public class MsSecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsSecurityApplication.class, args);
    }
}
