package com.lakhmann.budgetbot.telegram.miniapp;

import com.lakhmann.budgetbot.balance.MoneyFormatter;
import com.lakhmann.budgetbot.config.properties.JobsProperties;
import com.lakhmann.budgetbot.integration.ynab.YnabClient;
import com.lakhmann.budgetbot.integration.ynab.dto.YnabTransactionsResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
class MiniAppServiceTest {

    @Test
    void returnsSixLatestNonDeletedTransactionsWithFormattedFields() {
        YnabClient ynabClient = mock(YnabClient.class);
        MoneyFormatter moneyFormatter = mock(MoneyFormatter.class);
        JobsProperties jobs = new JobsProperties("0 0 * * * *", "UTC", "token");
        Clock clock = Clock.fixed(Instant.parse("2025-01-10T12:00:00Z"), ZoneOffset.UTC);

        when(ynabClient.getTransactionsSince(java.time.LocalDate.of(2024, 12, 11))).thenReturn(List.of(
                tx("1", "2025-01-10", -1000L, "Кофе", "Еда", false),
                tx("2", "2025-01-09", 1200L, "", "", false),
                tx("3", "2025-01-08", null, null, "Транспорт", false),
                tx("4", "2025-01-07", 4000L, "Супермаркет", null, false),
                tx("5", "2025-01-06", 5000L, "Подарок", "Разное", null),
                tx("6", "2025-01-05", 6000L, "Такси", "Транспорт", false),
                tx("7", "2025-01-04", 7000L, "Старый", "Архив", false),
                tx("8", "2025-01-03", 8000L, "Удаленный", "Архив", true)
        ));

        when(moneyFormatter.formatMilliunits(1000L)).thenReturn("₽1.00");
        when(moneyFormatter.formatMilliunits(1200L)).thenReturn("₽1.20");
        when(moneyFormatter.formatMilliunits(0L)).thenReturn("₽0.00");
        when(moneyFormatter.formatMilliunits(4000L)).thenReturn("₽4.00");
        when(moneyFormatter.formatMilliunits(5000L)).thenReturn("₽5.00");
        when(moneyFormatter.formatMilliunits(6000L)).thenReturn("₽6.00");

        MiniAppService service = new MiniAppService(ynabClient, moneyFormatter, clock, jobs);

        List<MiniAppTransactionDto> result = service.lastSixTransactions();

        assertThat(result).hasSize(6);
        assertThat(result).extracting(MiniAppTransactionDto::id)
                .containsExactly("1", "2", "3", "4", "5", "6");

        assertThat(result.get(0).title()).isEqualTo("Кофе");
        assertThat(result.get(0).secondaryText()).isEqualTo("Сегодня • Еда");
        assertThat(result.get(0).amount()).isEqualTo("₽1.00");

        assertThat(result.get(1).title()).isEqualTo("Транзакция");
        assertThat(result.get(1).secondaryText()).isEqualTo("Вчера • Без категории");
        assertThat(result.get(1).amount()).isEqualTo("₽1.20");

        assertThat(result.get(2).title()).isEqualTo("Транзакция");
        assertThat(result.get(2).secondaryText()).isEqualTo("8 янв. • Транспорт");
        assertThat(result.get(2).amount()).isEqualTo("₽0.00");

        assertThat(result.get(3).secondaryText()).isEqualTo("7 янв. • Без категории");
        assertThat(result.get(4).secondaryText()).isEqualTo("6 янв. • Разное");

        verify(ynabClient).getTransactionsSince(java.time.LocalDate.of(2024, 12, 11));
    }

    private static YnabTransactionsResponse.Transaction tx(
            String id,
            String date,
            Long amount,
            String payee,
            String category,
            Boolean deleted
    ) {
        return new YnabTransactionsResponse.Transaction(
                id,
                java.time.LocalDate.parse(date),
                amount,
                payee,
                category,
                deleted
        );
    }
}
