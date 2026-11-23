package org.your.gossips.gossipssecurity.adapter.out.persistence.impl;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.your.gossips.gossipssecurity.adapter.out.persistence.RefreshTokenPersistence;
import org.your.gossips.gossipssecurity.adapter.out.persistence.StoredRefreshToken;
import org.your.gossips.gossipssecurity.jooq.tables.records.RefreshTokensRecord;

import java.util.Optional;
import java.util.UUID;

import static org.your.gossips.gossipssecurity.jooq.tables.RefreshTokens.REFRESH_TOKENS;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository implements RefreshTokenPersistence {

    private final DSLContext dsl;

    @Override
    public Optional<StoredRefreshToken> findByHash(String tokenHash) {
        return dsl.selectFrom(REFRESH_TOKENS)
                .where(REFRESH_TOKENS.TOKEN_HASH.eq(tokenHash))
                .fetchOptionalInto(StoredRefreshToken.class);
    }

    @Override
    public StoredRefreshToken insert(StoredRefreshToken token) {
        RefreshTokensRecord record = dsl.newRecord(REFRESH_TOKENS, token);
        record.setId(UUID.randomUUID());
        return dsl.insertInto(REFRESH_TOKENS)
                .set(record)
                .returning()
                .fetchOneInto(StoredRefreshToken.class);
    }

    @Override
    public void revoke(UUID id) {
        dsl.update(REFRESH_TOKENS)
                .set(REFRESH_TOKENS.REVOKED_AT, org.jooq.impl.DSL.currentOffsetDateTime())
                .where(REFRESH_TOKENS.ID.eq(id))
                .execute();
    }
}
