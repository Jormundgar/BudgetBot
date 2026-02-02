package com.lakhmann.budgetbot.telegram.webhook;

import com.lakhmann.budgetbot.balance.BalanceService;
import com.lakhmann.budgetbot.config.properties.TelegramProperties;
import com.lakhmann.budgetbot.telegram.TelegramClient;
import com.lakhmann.budgetbot.telegram.TelegramMessages;
import com.lakhmann.budgetbot.telegram.recipients.RecipientsStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelegramWebhookController.class)
@Tag("slice")
class TelegramWebhookControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TelegramClient telegramClient;

    @MockBean
    private BalanceService balanceService;

    @MockBean
    private TelegramProperties telegramProperties;

    @MockBean
    private RecipientsStore recipientsStore;

    @Test
    void rejectsWhenSecretInvalid() throws Exception {
        when(telegramProperties.webhookSecret()).thenReturn("secret");

        mockMvc.perform(post("/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void handlesStartCommand() throws Exception {
        when(telegramProperties.webhookSecret()).thenReturn("secret");

        String payload = """
                {
                  "message": {
                    "chat": { "id": 99 },
                    "text": "/start"
                  }
                }
                """;

        mockMvc.perform(post("/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(recipientsStore).add(99L);
        verify(telegramClient).ensureBottomKeyboard(99L);
    }

    @Test
    void sendsBalanceOnCommand() throws Exception {
        when(telegramProperties.webhookSecret()).thenReturn("secret");
        when(balanceService.currentAvailableBalanceText()).thenReturn("balance");

        String payload = """
                {
                  "message": {
                    "chat": { "id": 42 },
                    "text": "%s"
                  }
                }
                """.formatted(TelegramMessages.BALANCE_COMMAND_TEXT);

        mockMvc.perform(post("/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(telegramClient).sendPlainMessage(42L, "balance");
    }
}
