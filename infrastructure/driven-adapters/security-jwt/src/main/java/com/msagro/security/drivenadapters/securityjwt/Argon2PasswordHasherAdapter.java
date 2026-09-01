package com.msagro.security.drivenadapters.securityjwt;

import com.msagro.security.usecase.gateway.security.PasswordHasherPort;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * Password hashing with Argon2id, behind a {@link DelegatingPasswordEncoder}.
 *
 * <p>The encoder writes {@code {argon2}...} in front of every new hash, so the algorithm that
 * produced a hash is recorded with the hash itself. Verification dispatches on that prefix,
 * which is what makes moving to stronger parameters — or a different algorithm entirely —
 * possible without a flag day: old hashes keep verifying, and {@link #needsUpgrade(String)}
 * tells the login flow to re-hash them the next time the raw password passes through.</p>
 *
 * <p>BCrypt is kept as a verifier for legacy hashes only. It is never used to produce one.</p>
 *
 * <p>Parameters follow the OWASP baseline for Argon2id: 19 MiB of memory, two iterations and a
 * parallelism of one, with a 16-byte random salt per password and a 32-byte output. The whole
 * encoded string — algorithm, parameters, salt and hash — is what lands in
 * {@code security_user.password_hash}; the raw password is never stored, encrypted or logged.</p>
 *
 * <p>Argon2 is deliberately expensive, so both operations are pushed to the bounded-elastic
 * scheduler and never run on an event-loop thread.</p>
 */
@Component
public class Argon2PasswordHasherAdapter implements PasswordHasherPort {

    /** Identifier written into every new hash. */
    private static final String CURRENT_ALGORITHM = "argon2";

    // OWASP baseline for Argon2id.
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    private static final int PARALLELISM = 1;
    private static final int MEMORY_KIB = 19 * 1024;
    private static final int ITERATIONS = 2;

    private final PasswordEncoder encoder;

    public Argon2PasswordHasherAdapter() {
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(
                SALT_LENGTH, HASH_LENGTH, PARALLELISM, MEMORY_KIB, ITERATIONS);

        Map<String, PasswordEncoder> encoders = Map.of(
                CURRENT_ALGORITHM, argon2,
                // Verification only: hashes migrated from an older system stay usable.
                "bcrypt", new BCryptPasswordEncoder(12));

        this.encoder = new DelegatingPasswordEncoder(CURRENT_ALGORITHM, encoders);
    }

    @Override
    public Mono<String> hash(String rawPassword) {
        return Mono.fromCallable(() -> encoder.encode(rawPassword))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> matches(String rawPassword, String passwordHash) {
        return Mono.fromCallable(() -> encoder.matches(rawPassword, passwordHash))
                .subscribeOn(Schedulers.boundedElastic())
                // A malformed stored hash is a failed match, not a 500.
                .onErrorReturn(false);
    }

    @Override
    public boolean needsUpgrade(String passwordHash) {
        return passwordHash != null && !passwordHash.startsWith("{" + CURRENT_ALGORITHM + "}");
    }
}
