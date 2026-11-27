package org.your.gossips.gossipsgateway.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.your.gossips.common.dto.Profile;
import org.your.gossips.common.dto.ProfileUpdateRequest;
import org.your.gossips.gossipsgateway.adapter.out.AppServiceClient;
import org.your.gossips.gossipsgateway.service.ProfileService;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final AppServiceClient appClient;

    @Override
    public Profile get(String username) {
        return appClient.getProfile(username);
    }

    @Override
    public Profile update(String username, ProfileUpdateRequest request) {
        return appClient.updateProfile(username, request);
    }
}
