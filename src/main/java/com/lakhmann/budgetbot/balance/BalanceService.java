package com.lakhmann.budgetbot.balance;

import com.lakhmann.budgetbot.config.properties.JobsProperties;
import com.lakhmann.budgetbot.integration.ynab.YnabClient;
import com.lakhmann.budgetbot.integration.ynab.dto.YnabMonthResponse;
import com.lakhmann.budgetbot.telegram.TelegramMessages;
import com.lakhmann.budgetbot.user.UserYnabAuthService;
import com.lakhmann.budgetbot.user.YnabUserSession;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

@Service
public class BalanceService {

    private final YnabClient ynabClient;
    private final MoneyFormatter moneyFormatter;
    private final JobsProperties jobs;
    private final UserYnabAuthService userYnabAuthService;

    public BalanceService(YnabClient ynabClient,
                          MoneyFormatter moneyFormatter,
                          JobsProperties jobs,
                          UserYnabAuthService userYnabAuthService) {
        this.ynabClient = ynabClient;
        this.moneyFormatter = moneyFormatter;
        this.jobs = jobs;
        this.userYnabAuthService = userYnabAuthService;
    }

    public String currentAvailableBalanceText(long userId) {
        long milli = getBalanceWithKnowledge(userId, null).valueMilli();
        return formatAvailableBalance(milli);
    }

    public BalanceSnapshot getBalanceWithKnowledge(
            long userId, Long lastServerKnowledge) {
        ZoneId zone = ZoneId.of(jobs.zone());
        LocalDate today = LocalDate.now(zone);
        YnabUserSession session = userYnabAuthService.sessionFor(userId);

        YearMonth target =  today.getDayOfMonth() <= 10
                ? YearMonth.from(today)
                : YearMonth.from(today).plusMonths(1);

        String monthParam = target.atDay(1).toString();

        YnabMonthResponse response = ynabClient.getMonth(
                session.accessToken(),
                session.budgetId(),
                monthParam,
                lastServerKnowledge);

        long toBeBudgetedMilli = response.data().month().toBeBudgetedMilliunits();
        Long serverKnowledge = response.data().serverKnowledge();

        return new BalanceSnapshot(toBeBudgetedMilli, serverKnowledge);
    }

    public String formatAvailableBalance(long milli) {
        String formatted = moneyFormatter.formatMilliunits(milli);
        return TelegramMessages.AVAILABLE_BALANCE_PREFIX + formatted;
    }

    public BalanceSnapshot forceRefreshSnapshot(long userId) {
        return getBalanceWithKnowledge(userId, null);
    }
}
