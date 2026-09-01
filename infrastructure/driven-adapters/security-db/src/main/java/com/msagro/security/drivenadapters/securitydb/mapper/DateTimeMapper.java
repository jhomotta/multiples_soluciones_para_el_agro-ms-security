package com.msagro.security.drivenadapters.securitydb.mapper;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Bridges the domain's {@link Instant} and the {@link OffsetDateTime} the PostgreSQL R2DBC
 * driver binds to {@code TIMESTAMPTZ}.
 *
 * <p>Everything is normalised to UTC on the way in and out. The domain never carries a zone,
 * and the column stores an absolute instant, so there is exactly one representation and no
 * chance for a local zone to sneak into stored data. MapStruct picks these methods up through
 * the {@code uses} attribute on every mapper.</p>
 */
@Component
public class DateTimeMapper {

    public OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    public Instant toInstant(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }
}
