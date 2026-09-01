package com.msagro.security.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Settings for the first-start administrator, bound from {@code security.bootstrap}.
 *
 * <p>The seed migration creates the company, the application and the RBAC catalogue, but it
 * cannot create a user: a password row needs a real Argon2id hash, and that can only be produced
 * by the running encoder. So the first administrator is created here instead.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.bootstrap")
public class BootstrapProperties {

    /** Create the administrator on start-up when it does not exist yet. */
    private boolean enabled = false;

    /** Code of the application the administrator is granted access to. */
    private String applicationCode = "AGRO_CORE";

    /** Code of the role assigned to the administrator. */
    private String roleCode = "ADMIN";

    private String username = "admin";

    /** Must be supplied through the environment; there is no default password on purpose. */
    private String password;

    private String email = "admin@msagro.local";

    private String identificationType = "CC";

    private String identificationNumber = "1000000000";

    private String firstName = "Administrador";

    private String lastName = "Sistema";

    /** Force a password change on the administrator's first login. */
    private boolean mustChangePassword = false;
}
