package org.your.gossips.gossipsgateway.adapter.out;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.your.gossips.common.ApiPaths;
import org.your.gossips.common.dto.Message;
import org.your.gossips.common.dto.MessageRequest;
import org.your.gossips.gossipsgateway.common.exception.ServiceUnavailableException;

import java.util.List;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class AppServiceClient {

    private final RestClient appRestClient;

    public List<Message> getMessages(String authorization) {
        Message[] body = execute(() -> appRestClient.get()
                .uri(ApiPaths.Messages.BASE)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(Message[].class));
        return List.of(body);
    }

    public Message sendMessage(String authorization, MessageRequest messageRequest) {
        return execute(() -> appRestClient.post()
                .uri(ApiPaths.Messages.BASE)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .body(messageRequest)
                .retrieve()
                .body(Message.class));
    }

    private <T> T execute(Supplier<T> call) {
        try {
            return call.get();
        } catch (ResourceAccessException e) {
            // Только сбой сети — 503. HTTP-ошибки downstream (401/409/...)
            // пробрасываются наверх и маппятся GlobalExceptionHandler'ом gateway.
            throw new ServiceUnavailableException("app", e);
        }
    }
}
