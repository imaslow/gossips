package org.your.gossips.gossipsapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.your.gossips.common.dto.Profile;
import org.your.gossips.common.dto.ProfileUpdateRequest;
import org.your.gossips.gossipsapp.adapter.out.model.StoredProfile;
import org.your.gossips.gossipsapp.adapter.out.persistence.ProfilePersistence;
import org.your.gossips.gossipsapp.mapper.ProfileMapper;
import org.your.gossips.gossipsapp.service.ProfileService;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfilePersistence profilePersistence;
    private final ProfileMapper profileMapper;

    @Override
    public Profile get(String username) {
        return profileMapper.toDto(profilePersistence.findByUsername(username)
                // Профиль ещё не создан — отдаём дефолт, не сохраняя
                .orElse(new StoredProfile(username, username, null, null, null)));
    }

    @Override
    public Profile update(String username, ProfileUpdateRequest request) {
        StoredProfile stored = profilePersistence.upsert(new StoredProfile(
                username,
                request.getDisplayName(),
                request.getBio(),
                null,
                null));
        return profileMapper.toDto(stored);
    }
}
