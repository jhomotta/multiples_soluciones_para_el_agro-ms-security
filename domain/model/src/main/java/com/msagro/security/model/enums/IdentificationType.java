package com.msagro.security.model.enums;

/**
 * Identification document types accepted by {@code person.identification_type}.
 * Stored as text so the catalogue can grow without a migration.
 */
public enum IdentificationType {
    /** Cédula de ciudadanía. */
    CC,
    /** Cédula de extranjería. */
    CE,
    /** Tarjeta de identidad. */
    TI,
    /** Pasaporte. */
    PASSPORT,
    /** NIT (persona jurídica). */
    NIT
}
