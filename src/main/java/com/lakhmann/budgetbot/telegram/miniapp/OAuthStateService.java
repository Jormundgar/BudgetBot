package com.lakhmann.budgetbot.telegram.miniapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class OAuthStateService {

    private final String secret;

    public OAuthStateService(@Value("${telegram.bot-token}") String botToken) {
        this.secret = botToken;
    }

    public String issue(long userId) {
        String payload = userId + ":" + System.currentTimeMillis();
        String signature = sign(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString((payload + ":" + signature).getBytes(StandardCharsets.UTF_8));
    }

    public long verifyAndExtractUserId(String state) {
        String decoded = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
        String[] parts = decoded.split(":");
        if (parts.length != 3) throw new IllegalArgumentException("bad state");
        String payload = parts[0] + ":" + parts[1];
        if (!sign(payload).equals(parts[2])) throw new IllegalArgumentException("bad signature");
        return Long.parseLong(parts[0]);
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
