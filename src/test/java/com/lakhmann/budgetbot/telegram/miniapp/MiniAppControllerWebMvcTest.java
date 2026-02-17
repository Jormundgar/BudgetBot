package com.lakhmann.budgetbot.telegram.miniapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakhmann.budgetbot.balance.BalanceService;
import com.lakhmann.budgetbot.balance.BalanceSnapshot;
import com.lakhmann.budgetbot.telegram.recipients.RecipientsStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MiniAppController.class)
@TestPropertySource(properties = "telegram.bot-token=test-bot-token")
@Tag("slice")
class MiniAppControllerWebMvcTest {

    private static final String BOT_TOKEN = "test-bot-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BalanceService balanceService;

    @MockBean
    private RecipientsStore recipientsStore;

    @MockBean
    private MiniAppService miniAppService;


    @Test
    void returnsUnauthorizedWhenInitDataIsInvalid() throws Exception {
        mockMvc.perform(post("/api/miniapp/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("bad-data")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsBalanceForAllowedUser() throws Exception {
        long userId = 1001L;
        String initData = signedInitData(userId, Instant.now().getEpochSecond());

        when(balanceService.getBalanceWithKnowledge(null)).thenReturn(new BalanceSnapshot(12345L, 10L));
        when(balanceService.formatAvailableBalance(12345L)).thenReturn("Доступный баланс: ₽12.345");
        when(recipientsStore.contains(userId)).thenReturn(true);

        mockMvc.perform(post("/api/miniapp/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(initData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceMilli").value(12345))
                .andExpect(jsonPath("$.balanceText").value("Доступный баланс: ₽12.345"))
                .andExpect(jsonPath("$.telegramUserId").value(1001));

        verify(recipientsStore).contains(userId);
    }

    @Test
    void returnsForbiddenForUnknownUser() throws Exception {
        long userId = 777L;
        String initData = signedInitData(userId, Instant.now().getEpochSecond());

        when(balanceService.getBalanceWithKnowledge(null)).thenReturn(new BalanceSnapshot(1L, 1L));
        when(recipientsStore.contains(userId)).thenReturn(false);

        mockMvc.perform(post("/api/miniapp/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(initData)))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshesBalanceForAllowedUser() throws Exception {
        long userId = 2024L;
        String initData = signedInitData(userId, Instant.now().getEpochSecond());

        when(balanceService.forceRefreshSnapshot()).thenReturn(new BalanceSnapshot(5000L, 99L));
        when(balanceService.formatAvailableBalance(5000L)).thenReturn("Доступный баланс: ₽5.000");
        when(recipientsStore.contains(anyLong())).thenReturn(true);

        mockMvc.perform(post("/api/miniapp/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(initData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceMilli").value(5000))
                .andExpect(jsonPath("$.telegramUserId").value(2024));
    }

    private String jsonBody(String initData) throws Exception {
        return objectMapper.writeValueAsString(Map.of("initData", initData));
    }

    private static String signedInitData(long userId, long authDate) throws Exception {
        TreeMap<String, String> params = new TreeMap<>(Map.of(
                "auth_date", String.valueOf(authDate),
                "query_id", "q-1",
                "user", "{\"id\":" + userId + ",\"first_name\":\"Test\"}"
        ));

        String dataCheckString = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secret = hmacSha256(BOT_TOKEN.getBytes(StandardCharsets.UTF_8), "WebAppData".getBytes(StandardCharsets.UTF_8));
        String hash = toHex(hmacSha256(dataCheckString.getBytes(StandardCharsets.UTF_8), secret));

        String encoded = params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));

        return encoded + "&hash=" + hash;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
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