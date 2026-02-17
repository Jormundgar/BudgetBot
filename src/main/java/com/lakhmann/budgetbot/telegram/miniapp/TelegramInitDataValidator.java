package com.lakhmann.budgetbot.telegram.miniapp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

public class TelegramInitDataValidator {

    private static final long MAX_AGE_SECONDS = 10 * 60;

    public static ValidationResult validate(String initData, String botToken) {
        if (initData == null || initData.isBlank()) {
            return ValidationResult.invalid("initData is empty");
        }
        try {
            Map<String, String> params = parseQuery(initData);

            String hash = params.remove("hash");
            if (hash == null || hash.isBlank()) return ValidationResult.invalid("hash missing");

            String authDateStr = params.get("auth_date");
            if (authDateStr == null) return ValidationResult.invalid("auth_date missing");

            long authDate = Long.parseLong(authDateStr);
            long now = Instant.now().getEpochSecond();
            if (now - authDate > MAX_AGE_SECONDS) return ValidationResult.invalid("initData too old");

            String dataCheckString = buildDataCheckString(params);

            byte[] secretKey = hmacSha256(
                    botToken.getBytes(StandardCharsets.UTF_8),
                    "WebAppData".getBytes(StandardCharsets.UTF_8)

            );

            String calculated = bytesToHex(hmacSha256(
                    dataCheckString.getBytes(StandardCharsets.UTF_8),
                    secretKey
            ));

            if (!calculated.equalsIgnoreCase(hash)) {
                return ValidationResult.invalid("hash mismatch");
            }

            return ValidationResult.valid(params);
        } catch (Exception e) {
            return ValidationResult.invalid("validation error: " + e.getMessage());
        }
    }

    private static Map<String, String> parseQuery(String initData) {
        Map<String, String> map = new HashMap<>();
        for (String pair : initData.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0) continue;
            String key = urlDecode(pair.substring(0, idx));
            String value = urlDecode(pair.substring(idx + 1));
            map.put(key, value);
        }
        return map;
    }

    private static String buildDataCheckString(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(k).append('=').append(params.get(k));
        }
        return sb.toString();
    }

    private static byte[] hmacSha256(byte[] data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    public record ValidationResult(boolean valid, String error, Map<String, String> params) {
        public static ValidationResult valid(Map<String, String> params) { return new ValidationResult(true, null, params); }
        public static ValidationResult invalid(String error) { return new ValidationResult(false, error, null); }
    }

    public static Long extractUserId(ValidationResult vr) {
        if (vr == null || !vr.valid() || vr.params() == null) return null;

        String userJson = vr.params().get("user");
        if (userJson == null || userJson.isBlank()) return null;
        int idx = userJson.indexOf("\"id\":");
        if (idx < 0) return null;
        int start = idx + 5;
        int end = start;
        while (end < userJson.length() && Character.isDigit(userJson.charAt(end))) end++;
        try {
            return Long.parseLong(userJson.substring(start, end));
        } catch (Exception e) {
            return null;
        }
    }
}
