package com.lakhmann.budgetbot.jobs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jobs.internal-schedule-enabled", havingValue = "true", matchIfMissing = true)
public class DailyBalanceScheduler {

    private final DailyBalanceJob dailyBalanceJob;

    public DailyBalanceScheduler(DailyBalanceJob dailyBalanceJob) {
        this.dailyBalanceJob = dailyBalanceJob;
    }

    @Scheduled(cron = "${jobs.cron}", zone = "${jobs.zone}")
    public void run() {
        dailyBalanceJob.run();
    }
}
