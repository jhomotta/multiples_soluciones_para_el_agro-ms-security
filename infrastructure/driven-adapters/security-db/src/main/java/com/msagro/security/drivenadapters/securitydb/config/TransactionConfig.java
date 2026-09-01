package com.msagro.security.drivenadapters.securitydb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * Exposes a {@link TransactionalOperator} over the reactive transaction manager Spring Boot
 * auto-configures for R2DBC.
 *
 * <p>{@code @Transactional} covers a whole method, which is the wrong shape whenever part of a
 * flow must be committed and the call must still fail — the security responses to a replayed
 * refresh token, for instance, where the revocation has to survive the 401 that follows it. An
 * operator lets a use case wrap exactly the operations that belong in one unit of work and place
 * the error outside it.</p>
 */
@Configuration
public class TransactionConfig {

    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }
}
