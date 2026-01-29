package com.lakhmann.budgetbot.telegram;

import com.lakhmann.budgetbot.telegram.recipients.RecipientsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TelegramNotificationService {

    public static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final TelegramClient telegramClient;
    private final RecipientsStore recipientsStore;

    public TelegramNotificationService(
            TelegramClient telegramClient,
            RecipientsStore recipientsStore) {

        this.telegramClient = telegramClient;
        this.recipientsStore = recipientsStore;
    }

    public List<Long> notifyAllRecipients(String text) {

        List<Long> recipients = recipientsStore.listAll();
        if (recipients.isEmpty()) {
            log.warn("No recipients in DynamoDB, skipping notify");
            return recipients;
        }

        for (Long chatID : recipients) {
            telegramClient.sendPlainMessage(chatID, text);
        }

        return recipients;
    }
}
