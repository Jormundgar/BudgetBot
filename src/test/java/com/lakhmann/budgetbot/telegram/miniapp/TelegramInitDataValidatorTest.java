package com.lakhmann.budgetbot.telegram.miniapp;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class TelegramInitDataValidatorTest {

    private static final String BOT_TOKEN = "bot-token";

    @Test
    void validatesSignedInitDataAndExtractsUserId() throws Exception {
        long now = Instant.now().getEpochSecond();
        String userJson = "{\"id\":123456,\"first_name\":\"Ivan\"}";

        String initData = sign(Map.of(
                "auth_date", String.valueOf(now),
                "query_id", "AAEAAAE",
                "user", userJson
        ));

        TelegramInitDataValidator.ValidationResult result = TelegramInitDataValidator.validate(initData, BOT_TOKEN);

        assertThat(result.valid()).isTrue();
        assertThat(result.error()).isNull();
        assertThat(TelegramInitDataValidator.extractUserId(result)).isEqualTo(123456L);
    }

    @Test
    void rejectsInitDataWithInvalidHash() {
        long now = Instant.now().getEpochSecond();
        String initData = "auth_date=" + now + "&user=%7B%22id%22%3A1%7D&hash=deadbeef";

        TelegramInitDataValidator.ValidationResult result = TelegramInitDataValidator.validate(initData, BOT_TOKEN);

        assertThat(result.valid()).isFalse();
        assertThat(result.error()).contains("hash mismatch");
    }

    @Test
    void rejectsTooOldInitData() throws Exception {
        long oldAuthDate = Instant.now().minusSeconds(11 * 60).getEpochSecond();
        String initData = sign(Map.of(
                "auth_date", String.valueOf(oldAuthDate),
                "user", "{\"id\":999}"
        ));

        TelegramInitDataValidator.ValidationResult result = TelegramInitDataValidator.validate(initData, BOT_TOKEN);

        assertThat(result.valid()).isFalse();
        assertThat(result.error()).contains("initData too old");
    }

    private static String sign(Map<String, String> params) throws Exception {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        String dataCheckString = sorted.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secret = hmacSha256(BOT_TOKEN.getBytes(StandardCharsets.UTF_8), "WebAppData".getBytes(StandardCharsets.UTF_8));
        String hash = toHex(hmacSha256(dataCheckString.getBytes(StandardCharsets.UTF_8), secret));

        return sorted.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&")) + "&hash=" + hash;
    }

    private static byte[] hmacSha256(byte[] data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}