package org.your.gossips.gossipsapp.adapter.in;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.your.gossips.common.dto.Profile;
import org.your.gossips.common.dto.ProfileUpdateRequest;
import org.your.gossips.gossipsapp.api.ProfilesApi;
import org.your.gossips.gossipsapp.service.ProfileService;

@RestController
@RequiredArgsConstructor
public class ProfileController implements ProfilesApi {

    private final ProfileService profileService;

    @Override
    public ResponseEntity<Profile> getMyProfile(String xUsername) {
        return ResponseEntity.ok(profileService.get(xUsername));
    }

    @Override
    public ResponseEntity<Profile> updateMyProfile(String xUsername, ProfileUpdateRequest profileUpdateRequest) {
        return ResponseEntity.ok(profileService.update(xUsername, profileUpdateRequest));
    }
}
