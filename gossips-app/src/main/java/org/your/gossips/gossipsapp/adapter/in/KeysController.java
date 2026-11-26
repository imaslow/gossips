package org.your.gossips.gossipsapp.adapter.in;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.your.gossips.common.dto.KeyVerifyRequest;
import org.your.gossips.common.dto.PublicKeyInfo;
import org.your.gossips.common.dto.PublicKeyUpsertRequest;
import org.your.gossips.common.dto.VerificationInfo;
import org.your.gossips.gossipsapp.api.KeysApi;
import org.your.gossips.gossipsapp.service.KeyService;

@RestController
@RequiredArgsConstructor
public class KeysController implements KeysApi {

    private final KeyService keyService;

    @Override
    public ResponseEntity<PublicKeyInfo> publishMyKey(String xUsername, PublicKeyUpsertRequest publicKeyUpsertRequest) {
        return ResponseEntity.ok(keyService.publish(xUsername, publicKeyUpsertRequest));
    }

    @Override
    public ResponseEntity<PublicKeyInfo> getMyKey(String xUsername) {
        return ResponseEntity.ok(keyService.getOwn(xUsername));
    }

    @Override
    public ResponseEntity<PublicKeyInfo> getUserKey(String xUsername, String username) {
        return ResponseEntity.ok(keyService.getFor(xUsername, username));
    }

    @Override
    public ResponseEntity<VerificationInfo> verifyUserKey(String xUsername, String username, KeyVerifyRequest keyVerifyRequest) {
        return ResponseEntity.ok(keyService.verify(xUsername, username, keyVerifyRequest));
    }

    @Override
    public ResponseEntity<Void> unverifyUserKey(String xUsername, String username) {
        keyService.unverify(xUsername, username);
        return ResponseEntity.noContent().build();
    }
}
