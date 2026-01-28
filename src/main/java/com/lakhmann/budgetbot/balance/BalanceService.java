package com.lakhmann.budgetbot.balance;

import com.lakhmann.budgetbot.config.properties.JobsProperties;
import com.lakhmann.budgetbot.integration.ynab.YnabClient;
import com.lakhmann.budgetbot.integration.ynab.dto.YnabMonthResponse;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.ZoneId;

@Service
public class BalanceService {

    private final YnabClient ynabClient;
    private final MoneyFormatter moneyFormatter;
    private final JobsProperties jobs;

    public BalanceService(YnabClient ynabClient, MoneyFormatter moneyFormatter, JobsProperties jobs) {
        this.ynabClient = ynabClient;
        this.moneyFormatter = moneyFormatter;
        this.jobs = jobs;
    }

    public String currentAvailableBalanceText() {
        long milli = getBalanceWithKnowledge(null).valueMilli();
        return formatAvailableBalance(milli);
    }

    public BalanceSnapshot getBalanceWithKnowledge(Long lastServerKnowledge) {
        ZoneId zone = ZoneId.of(jobs.zone());
        YearMonth target =  YearMonth.now(zone).plusMonths(1);
        String monthParam = target.atDay(1).toString();

        YnabMonthResponse response = ynabClient.getMonth(monthParam, lastServerKnowledge);

        long toBeBudgetedMilli = response.data().month().toBeBudgetedMilliunits();
        Long serverKnowledge = response.data().serverKnowledge();

        return new BalanceSnapshot(toBeBudgetedMilli, serverKnowledge);
    }

    public String formatAvailableBalance(long milli) {
        String formatted = moneyFormatter.formatMilliunits(milli);
        return "Доступный баланс: " + formatted;
    }
}
