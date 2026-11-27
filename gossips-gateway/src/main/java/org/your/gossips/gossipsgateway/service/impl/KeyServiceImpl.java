package org.your.gossips.gossipsgateway.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.your.gossips.common.dto.KeyVerifyRequest;
import org.your.gossips.common.dto.PublicKeyInfo;
import org.your.gossips.common.dto.PublicKeyUpsertRequest;
import org.your.gossips.common.dto.VerificationInfo;
import org.your.gossips.gossipsgateway.adapter.out.AppServiceClient;
import org.your.gossips.gossipsgateway.service.KeyService;

@Service
@RequiredArgsConstructor
public class KeyServiceImpl implements KeyService {

    private final AppServiceClient appClient;

    @Override
    public PublicKeyInfo publish(String username, PublicKeyUpsertRequest request) {
        return appClient.publishKey(username, request);
    }

    @Override
    public PublicKeyInfo getOwn(String username) {
        return appClient.getOwnKey(username);
    }

    @Override
    public PublicKeyInfo getFor(String caller, String username) {
        return appClient.getUserKey(caller, username);
    }

    @Override
    public VerificationInfo verify(String caller, String username, KeyVerifyRequest request) {
        return appClient.verifyKey(caller, username, request);
    }

    @Override
    public void unverify(String caller, String username) {
        appClient.unverifyKey(caller, username);
    }
}
