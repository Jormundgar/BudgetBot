package com.lakhmann.budgetbot.telegram.miniapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class OAuthStateService {

    private final String secret;
    private final Duration ttl;

    public OAuthStateService(@Value("${telegram.bot-token}") String botToken,
                             @Value("${ynab.oauth-state-ttl-seconds:600}") long ttlSeconds) {
        this.secret = botToken;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public String issue(long userId) {
        String payload = userId + ":" + Instant.now().toEpochMilli();
        String signature = sign(payload);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((payload + ":" + signature).getBytes(StandardCharsets.UTF_8));
    }

    public long verifyAndExtractUserId(String state) {
        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("bad state", e);
        }

        String[] parts = decoded.split(":", 3);

        if (parts.length != 3) throw new IllegalArgumentException("bad state");
        String payload = parts[0] + ":" + parts[1];

        if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("bad signature");
        }

        Instant issuedAt;
        try {
            issuedAt = Instant.ofEpochMilli(Long.parseLong(parts[1]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad state", e);
        }

        if (issuedAt.plus(ttl).isBefore(Instant.now())) throw new IllegalArgumentException("state expired");

        try {
            return Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad state", e);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
