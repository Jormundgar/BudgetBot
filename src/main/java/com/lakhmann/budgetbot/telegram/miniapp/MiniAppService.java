package com.lakhmann.budgetbot.telegram.miniapp;

import com.lakhmann.budgetbot.balance.MoneyFormatter;
import com.lakhmann.budgetbot.config.properties.JobsProperties;
import com.lakhmann.budgetbot.integration.ynab.YnabClient;
import com.lakhmann.budgetbot.integration.ynab.dto.YnabTransactionsResponse;

import com.lakhmann.budgetbot.user.UserYnabAuthService;
import com.lakhmann.budgetbot.user.YnabUserSession;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class MiniAppService {

    private final YnabClient ynabClient;
    private final MoneyFormatter moneyFormatter;
    private final Clock clock;
    private final JobsProperties jobs;
    private final UserYnabAuthService authService;

    public MiniAppService(
            YnabClient ynabClient,
            MoneyFormatter moneyFormatter,
            Clock clock,
            JobsProperties jobs,
            UserYnabAuthService authService
    ) {
        this.ynabClient = ynabClient;
        this.moneyFormatter = moneyFormatter;
        this.clock = clock;
        this.jobs = jobs;
        this.authService = authService;
    }

    public List<MiniAppTransactionDto> lastSixTransactions(long userId) {
        ZoneId zone = ZoneId.of(jobs.zone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate since = today.minusDays(30);
        YnabUserSession session = authService.sessionFor(userId);

        List<YnabTransactionsResponse.Transaction> tx = ynabClient.getTransactionsSince(
                session.accessToken(), session.budgetId(), since);

        return tx.stream()
                .filter(t -> t.deleted() == null || !t.deleted())
                .sorted(Comparator
                        .comparing(YnabTransactionsResponse.Transaction::date, Comparator.reverseOrder())
                        .thenComparing(YnabTransactionsResponse.Transaction::id, Comparator.reverseOrder()))
                .limit(6)
                .map(t -> toDto(t, today))
                .toList();
    }

    private MiniAppTransactionDto toDto(YnabTransactionsResponse.Transaction t, LocalDate today) {
        String title = (t.payeeName() != null && !t.payeeName().isBlank())
                ? t.payeeName()
                : "Транзакция";

        String category = (t.categoryName() != null && !t.categoryName().isBlank())
                ? t.categoryName()
                : "Без категории";

        String secondaryText = formatDateLabel(t.date(), today) + " • " + category;

        long rawAmount = (t.amount() == null) ? 0L : t.amount();
        Boolean income = (rawAmount == 0L) ? null : rawAmount > 0;
        long amountMilli = Math.abs(rawAmount);
        String amount = moneyFormatter.formatMilliunits(amountMilli);

        return new MiniAppTransactionDto(t.id(), title, secondaryText, amount, income);
    }

    private String formatDateLabel(LocalDate date, LocalDate today) {
        if (date == null) return "";

        if (date.equals(today)) return "Сегодня";
        if (date.equals(today.minusDays(1))) return "Вчера";

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru"));
        return date.format(fmt);
    }
}
