package com.msagro.security.drivenadapters.securitydb.repository;

import com.msagro.security.drivenadapters.securitydb.entity.SecurityUserEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Repository
public interface SecurityUserRepository extends ReactiveCrudRepository<SecurityUserEntity, Long> {

    /** Matches the {@code uq_security_user_username_lower} unique index. */
    @Query("SELECT * FROM security_user WHERE LOWER(username) = LOWER(:username)")
    Mono<SecurityUserEntity> findByUsernameIgnoreCase(@Param("username") String username);

    @Query("SELECT EXISTS(SELECT 1 FROM security_user WHERE LOWER(username) = LOWER(:username))")
    Mono<Boolean> existsByUsernameIgnoreCase(@Param("username") String username);

    Mono<SecurityUserEntity> findByPersonId(Long personId);

    /** A successful login clears the failure counter and lifts any expired lock. */
    @Modifying
    @Query("""
            UPDATE security_user
               SET last_login_at = :when,
                   failed_attempts = 0,
                   locked = FALSE,
                   locked_until = NULL,
                   updated_at = now()
             WHERE id = :userId
            """)
    Mono<Integer> registerSuccessfulLogin(@Param("userId") Long userId,
                                          @Param("when") OffsetDateTime when);

    /** Increments the counter atomically and hands back the new value. */
    @Query("""
            UPDATE security_user
               SET failed_attempts = failed_attempts + 1,
                   updated_at = now()
             WHERE id = :userId
            RETURNING failed_attempts
            """)
    Mono<Short> registerFailedAttempt(@Param("userId") Long userId);

    @Modifying
    @Query("""
            UPDATE security_user
               SET locked = TRUE,
                   locked_until = :until,
                   updated_at = now()
             WHERE id = :userId
            """)
    Mono<Integer> lockUntil(@Param("userId") Long userId, @Param("until") OffsetDateTime until);

    /**
     * Replaces the password and resets everything a password change should reset, bumping
     * {@code security_stamp} so tokens issued before this moment can be recognised as stale.
     */
    @Modifying
    @Query("""
            UPDATE security_user
               SET password_hash = :passwordHash,
                   password_changed_at = :when,
                   must_change_password = FALSE,
                   credentials_expired = FALSE,
                   failed_attempts = 0,
                   locked = FALSE,
                   locked_until = NULL,
                   security_stamp = security_stamp + 1,
                   updated_at = now()
             WHERE id = :userId
            """)
    Mono<Integer> updatePassword(@Param("userId") Long userId,
                                 @Param("passwordHash") String passwordHash,
                                 @Param("when") OffsetDateTime when);

    @Modifying
    @Query("""
            UPDATE security_user
               SET security_stamp = security_stamp + 1,
                   updated_at = now()
             WHERE id = :userId
            """)
    Mono<Integer> bumpSecurityStamp(@Param("userId") Long userId);
}
