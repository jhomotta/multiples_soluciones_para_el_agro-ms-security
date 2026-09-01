package com.msagro.security.drivenadapters.securitydb.adapter;

import org.springframework.r2dbc.core.DatabaseClient;

/**
 * Small helper for the adapters that write PostgreSQL-specific types.
 *
 * <p>{@code DatabaseClient.bind} rejects a null value: a null has to be declared with its Java
 * type through {@code bindNull}, otherwise the driver cannot infer the parameter type. Every
 * column in this schema that can be absent (ip address, user agent, device, metadata) would
 * need that dance at each call site, so it is done once here.</p>
 */
final class SqlBinder {

    private SqlBinder() {
    }

    /** Binds {@code value}, or a typed NULL when it is absent. */
    static <T> DatabaseClient.GenericExecuteSpec bind(DatabaseClient.GenericExecuteSpec spec,
                                                      String name,
                                                      T value,
                                                      Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }
}
