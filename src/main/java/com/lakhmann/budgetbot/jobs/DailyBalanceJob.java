package com.lakhmann.budgetbot.jobs;

import com.lakhmann.budgetbot.balance.BalanceService;
import com.lakhmann.budgetbot.telegram.TelegramNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyBalanceJob {

    private final BalanceService balanceService;
    private final TelegramNotificationService notificationService;

    public DailyBalanceJob(
            BalanceService balanceService,
            TelegramNotificationService notificationService
    ) {
        this.balanceService = balanceService;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${jobs.cron}", zone = "${jobs.zone}")
    public void run() {
        String text = balanceService.currentAvailableBalanceText();
        notificationService.notifyAllRecipients(text);
    }
}
