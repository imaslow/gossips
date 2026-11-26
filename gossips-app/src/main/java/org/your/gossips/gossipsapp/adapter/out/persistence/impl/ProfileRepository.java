package org.your.gossips.gossipsapp.adapter.out.persistence.impl;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.your.gossips.gossipsapp.adapter.out.persistence.ProfilePersistence;
import org.your.gossips.gossipsapp.adapter.out.model.StoredProfile;
import org.your.gossips.gossipsapp.jooq.tables.records.ProfilesRecord;

import java.util.Optional;

import static org.your.gossips.gossipsapp.jooq.tables.Profiles.PROFILES;

@Repository
@RequiredArgsConstructor
public class ProfileRepository implements ProfilePersistence {

    private final DSLContext dsl;

    @Override
    public Optional<StoredProfile> findByUsername(String username) {
        return dsl.selectFrom(PROFILES)
                .where(PROFILES.USERNAME.eq(username))
                .fetchOptionalInto(StoredProfile.class);
    }

    @Override
    public StoredProfile upsert(StoredProfile profile) {
        ProfilesRecord record = dsl.newRecord(PROFILES, profile);
        return dsl.insertInto(PROFILES)
                .set(record)
                .onConflict(PROFILES.USERNAME)
                .doUpdate()
                .set(PROFILES.DISPLAY_NAME, profile.displayName())
                .set(PROFILES.BIO, profile.bio())
                .set(PROFILES.UPDATED_AT, DSL.currentOffsetDateTime())
                .returning()
                .fetchOneInto(StoredProfile.class);
    }
}
