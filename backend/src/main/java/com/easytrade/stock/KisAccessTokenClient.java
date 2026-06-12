package com.easytrade.stock;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "app.kis.enabled", havingValue = "true")
public class KisAccessTokenClient {

    private final RestClient restClient;
    private final String appKey;
    private final String appSecret;
    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    public KisAccessTokenClient(
            @Value("${app.kis.base-url}") String baseUrl,
            @Value("${app.kis.app-key}") String appKey,
            @Value("${app.kis.app-secret}") String appSecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory())
                .build();
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return factory;
    }

    public synchronized String getAccessToken() {
        if (accessToken != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) {
            return accessToken;
        }

        TokenResponse response = restClient.post()
                .uri("/oauth2/tokenP")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TokenRequest("client_credentials", appKey, appSecret))
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("?쒓뎅?ъ옄利앷텒 ?묎렐 ?좏겙 諛쒓툒???ㅽ뙣?덉뒿?덈떎.");
        }

        this.accessToken = response.accessToken();
        this.expiresAt = Instant.now().plusSeconds(Math.max(response.expiresIn(), 60));
        return accessToken;
    }

    private record TokenRequest(
            @JsonProperty("grant_type") String grantType,
            @JsonProperty("appkey") String appKey,
            @JsonProperty("appsecret") String appSecret
    ) {
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }
}

