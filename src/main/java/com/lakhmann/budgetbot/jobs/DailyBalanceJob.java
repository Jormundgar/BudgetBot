package com.lakhmann.budgetbot.jobs;

import com.lakhmann.budgetbot.balance.BalanceService;
import com.lakhmann.budgetbot.telegram.TelegramClient;
import com.lakhmann.budgetbot.user.UserYnabConnectionStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyBalanceJob {

    private final BalanceService balanceService;
    private final UserYnabConnectionStore connectionStore;
    private final TelegramClient telegramClient;

    public DailyBalanceJob(
            BalanceService balanceService,
            UserYnabConnectionStore connectionStore,
            TelegramClient telegramClient
    ) {
        this.balanceService = balanceService;
        this.connectionStore = connectionStore;
        this.telegramClient = telegramClient;
    }

    @Scheduled(cron = "${jobs.cron}", zone = "${jobs.zone}")
    public void run() {
        connectionStore.listConnectedUserIds()
                .forEach(userId -> telegramClient.sendPlainMessage(userId, balanceService.currentAvailableBalanceText(userId)));
    }
}
