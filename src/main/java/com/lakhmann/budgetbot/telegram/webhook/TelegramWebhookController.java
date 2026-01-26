package com.lakhmann.budgetbot.telegram.webhook;

import com.lakhmann.budgetbot.config.properties.TelegramProperties;
import com.lakhmann.budgetbot.balance.BalanceService;
import com.lakhmann.budgetbot.telegram.TelegramClient;
import com.lakhmann.budgetbot.telegram.dto.TelegramUpdate;
import com.lakhmann.budgetbot.telegram.recipients.RecipientsStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/telegram")
public class TelegramWebhookController {

    private final TelegramClient telegramClient;
    private final BalanceService balanceService;
    private final TelegramProperties props;
    private final RecipientsStore recipientsStore;

    public TelegramWebhookController(
            TelegramClient telegramClient,
            BalanceService balanceService,
            TelegramProperties props,
            RecipientsStore recipientsStore
    ) {
        this.telegramClient = telegramClient;
        this.balanceService = balanceService;
        this.props = props;
        this.recipientsStore = recipientsStore;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> onUpdate(
            @RequestHeader("X-Telegram-Bot-Api-Secret-Token") String secret,
            @RequestBody TelegramUpdate update
    ) {
        if (!props.webhookSecret().equals(secret)) {
            return ResponseEntity.status(403).build();
        }

        if (update == null || update.message() == null) {
            return ResponseEntity.ok().build();
        }

        long chatId = update.message().chat().id();
        String text = update.message().text();

        if ("/start".equals(text)) {
            recipientsStore.add(chatId);
            telegramClient.ensureBottomKeyboard(chatId);
            return ResponseEntity.ok().build();
        }

        if ("Получить актуальный баланс".equals(text)) {
            telegramClient.sendPlainMessage(
                    chatId,
                    balanceService.currentAvailableBalanceText()
            );
        }

        return ResponseEntity.ok().build();
    }
}
