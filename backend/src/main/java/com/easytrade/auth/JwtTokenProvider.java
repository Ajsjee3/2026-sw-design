package com.easytrade.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long expirationMinutes;

    public JwtTokenProvider(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    public String createToken(String email) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = Map.of(
                    "sub", email,
                    "exp", Instant.now().plusSeconds(expirationMinutes * 60).getEpochSecond()
            );

            String headerPart = encode(objectMapper.writeValueAsBytes(header));
            String payloadPart = encode(objectMapper.writeValueAsBytes(payload));
            String signaturePart = sign(headerPart + "." + payloadPart);

            return headerPart + "." + payloadPart + "." + signaturePart;
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 생성에 실패했습니다.", exception);
        }
    }

    public String getEmail(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !sign(parts[0] + "." + parts[1]).equals(parts[2])) {
                return null;
            }

            Map<String, Object> payload = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    new TypeReference<>() {
                    }
            );

            Number expiration = (Number) payload.get("exp");
            if (expiration == null || expiration.longValue() < Instant.now().getEpochSecond()) {
                return null;
            }

            Object subject = payload.get("sub");
            return subject == null ? null : subject.toString();
        } catch (Exception exception) {
            return null;
        }
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return encode(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
