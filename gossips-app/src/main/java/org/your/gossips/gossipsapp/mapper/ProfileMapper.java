package org.your.gossips.gossipsapp.mapper;

import org.mapstruct.Mapper;
import org.your.gossips.common.dto.Profile;
import org.your.gossips.gossipsapp.adapter.out.model.StoredProfile;

/**
 * Маппинг хранимого профиля в API-DTO (имена полей совпадают).
 */
@Mapper(componentModel = "spring")
public interface ProfileMapper {

    Profile toDto(StoredProfile stored);
}
