package com.lakhmann.budgetbot.telegram.miniapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakhmann.budgetbot.balance.BalanceService;
import com.lakhmann.budgetbot.balance.BalanceSnapshot;
import com.lakhmann.budgetbot.integration.ynab.YnabClient;
import com.lakhmann.budgetbot.integration.ynab.oauth.YnabOAuthService;
import com.lakhmann.budgetbot.integration.ynab.oauth.YnabTokenResponse;
import com.lakhmann.budgetbot.telegram.recipients.RecipientsStore;
import com.lakhmann.budgetbot.user.UserYnabConnectionStore;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(MiniAppController.class)
@TestPropertySource(properties = {
        "telegram.bot-token=test-bot-token",
        "telegram.miniapp-url=https://mini.app"
})
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

    @MockBean
    private OAuthStateService stateService;

    @MockBean
    private YnabOAuthService oauthService;

    @MockBean
    private UserYnabConnectionStore connectionStore;

    @MockBean
    private YnabClient ynabClient;


    @Test
    void startsOauthForAllowedUser() throws Exception {
        long userId = 1001L;
        String initData = signedInitData(userId, Instant.now().getEpochSecond());

        when(recipientsStore.contains(userId)).thenReturn(true);
        when(stateService.issue(userId)).thenReturn("state");
        when(oauthService.buildAuthorizeUrl("state")).thenReturn("https://auth.url");

        mockMvc.perform(post("/api/miniapp/oauth/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(initData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://auth.url"));
    }

    @Test
    void handlesOauthCallbackAndSavesConnection() throws Exception {
        when(stateService.verifyAndExtractUserId("ok-state")).thenReturn(42L);
        when(oauthService.exchangeCode("code123")).thenReturn(new YnabTokenResponse("access", "refresh"));
        when(ynabClient.getPrimaryBudgetId("access")).thenReturn("budget-1");

        mockMvc.perform(get("/api/miniapp/oauth/callback")
                        .param("code", "code123")
                        .param("state", "ok-state"))
                .andExpect(status().isFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Location", "https://mini.app"));

        verify(connectionStore).save(any());
    }

    @Test
    void returnsLastTransactionsForAllowedUser() throws Exception {
        long userId = 2024L;
        String initData = signedInitData(userId, Instant.now().getEpochSecond());
        when(recipientsStore.contains(userId)).thenReturn(true);
        when(miniAppService.lastSixTransactions(userId)).thenReturn(List.of(
                new MiniAppTransactionDto("t1", "Кофе", "Сегодня • Еда", "₽1.00")
        ));

        mockMvc.perform(post("/api/miniapp/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(initData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("t1"));

        verify(miniAppService).lastSixTransactions(userId);
    }

    @Test
    void returnsForbiddenForNotAllowedUser() throws Exception {
        long userId = 2025L;
        String initData = signedInitData(userId, Instant.now().getEpochSecond());

        when(recipientsStore.contains(userId)).thenReturn(false);
        when(balanceService.getBalanceWithKnowledge(eq(userId), eq(null))).thenReturn(new BalanceSnapshot(1L, 1L));

        mockMvc.perform(post("/api/miniapp/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(initData)))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshesBalanceForAllowedUser() throws Exception {
        long userId = 2024L;
        String initData = signedInitData(userId, Instant.now().getEpochSecond());

        when(balanceService.forceRefreshSnapshot(anyLong())).thenReturn(new BalanceSnapshot(5000L, 99L));
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
