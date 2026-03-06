package com.lakhmann.budgetbot.telegram.webhook;

import com.lakhmann.budgetbot.config.properties.TelegramProperties;
import com.lakhmann.budgetbot.telegram.TelegramClient;
import com.lakhmann.budgetbot.telegram.recipients.RecipientsStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
class TelegramWebhookControllerWebMvcTest {

    private MockMvc mockMvc(TelegramClient telegramClient,
                            TelegramProperties telegramProperties,
                            RecipientsStore recipientsStore) {
        return MockMvcBuilders.standaloneSetup(
                new TelegramWebhookController(telegramClient, telegramProperties, recipientsStore)
        ).setMessageConverters(new MappingJackson2HttpMessageConverter()).build();
    }

    @Test
    void rejectsWhenSecretInvalid() throws Exception {
        TelegramClient telegramClient = mock(TelegramClient.class);
        TelegramProperties telegramProperties = new TelegramProperties(
                "https://api.telegram.org",
                "bot-token",
                "",
                "secret",
                "https://mini.app"
        );
        RecipientsStore recipientsStore = mock(RecipientsStore.class);

        mockMvc(telegramClient, telegramProperties, recipientsStore)
                .perform(post("/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void handlesStartCommand() throws Exception {
        TelegramClient telegramClient = mock(TelegramClient.class);
        TelegramProperties telegramProperties = new TelegramProperties(
                "https://api.telegram.org",
                "bot-token",
                "",
                "secret",
                "https://mini.app"
        );
        RecipientsStore recipientsStore = mock(RecipientsStore.class);

        String payload = """
                {
                  "message": {
                    "chat": { "id": 99 },
                    "text": "/start"
                  }
                }
                """;

        mockMvc(telegramClient, telegramProperties, recipientsStore)
                .perform(post("/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(recipientsStore).add(99L);
        verify(telegramClient).ensureBottomKeyboard(99L);
    }

    @Test
    void ignoresNonStartCommand() throws Exception {
        TelegramClient telegramClient = mock(TelegramClient.class);
        TelegramProperties telegramProperties = new TelegramProperties(
                "https://api.telegram.org",
                "bot-token",
                "",
                "secret",
                "https://mini.app"
        );
        RecipientsStore recipientsStore = mock(RecipientsStore.class);

        String payload = """
                {
                  "message": {
                    "chat": { "id": 42 },
                    "text": "/balance"
                  }
                }
                """;

        mockMvc(telegramClient, telegramProperties, recipientsStore)
                .perform(post("/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(recipientsStore, never()).add(42L);
        verify(telegramClient, never()).ensureBottomKeyboard(42L);
    }
}
