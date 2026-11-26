package org.your.gossips.gossipsapp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.your.gossips.common.dto.PublicKeyInfo;
import org.your.gossips.common.dto.VerificationInfo;
import org.your.gossips.gossipsapp.adapter.out.model.StoredKey;
import org.your.gossips.gossipsapp.adapter.out.model.StoredKeyVerification;

@Mapper(componentModel = "spring")
public interface KeyMapper {

    /**
     * Базовый маппинг ключа; поле verified выставляет сервис
     * (зависит от того, кто запрашивает).
     */
    PublicKeyInfo toDto(StoredKey storedKey);

    @Mapping(source = "verifiedUsername", target = "username")
    VerificationInfo toDto(StoredKeyVerification verification);
}
