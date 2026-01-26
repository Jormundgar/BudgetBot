package com.lakhmann.budgetbot.jobs;

import com.lakhmann.budgetbot.balance.BalanceService;
import com.lakhmann.budgetbot.telegram.TelegramClient;
import com.lakhmann.budgetbot.telegram.recipients.RecipientsStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyBalanceJob {

    private final TelegramClient telegramClient;
    private final BalanceService balanceService;
    private final RecipientsStore recipientsStore;

    public DailyBalanceJob(
            TelegramClient telegramClient,
            BalanceService balanceService,
            RecipientsStore recipientsStore
    ) {
        this.telegramClient = telegramClient;
        this.balanceService = balanceService;
        this.recipientsStore = recipientsStore;
    }

    @Scheduled(cron = "${jobs.cron}", zone = "${jobs.zone}")
    public void run() {
        var recipients = recipientsStore.listAll();
        if (recipients.isEmpty()) return;

        String text = balanceService.currentAvailableBalanceText();

        for (long chatId : recipients) {
            telegramClient.sendPlainMessage(chatId, text);
        }
    }
}
