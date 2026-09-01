package com.msagro.security.drivenadapters.securitydb.repository;

import com.msagro.security.drivenadapters.securitydb.entity.PersonEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface PersonRepository extends ReactiveCrudRepository<PersonEntity, Long> {

    Mono<PersonEntity> findByIdentificationTypeAndIdentificationNumber(String identificationType,
                                                                       String identificationNumber);

    /** Matches the {@code uq_person_email_lower} unique index, so the index is actually used. */
    @Query("SELECT * FROM person WHERE LOWER(email) = LOWER(:email)")
    Mono<PersonEntity> findByEmailIgnoreCase(@Param("email") String email);
}
