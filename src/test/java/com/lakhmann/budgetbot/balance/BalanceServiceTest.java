package com.lakhmann.budgetbot.balance;

import com.lakhmann.budgetbot.config.properties.JobsProperties;
import com.lakhmann.budgetbot.integration.ynab.YnabClient;
import com.lakhmann.budgetbot.integration.ynab.dto.YnabMonthResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
class BalanceServiceTest {

    @Test
    void returnsSnapshotFromYnabResponse() {
        YnabClient ynabClient = mock(YnabClient.class);
        MoneyFormatter moneyFormatter = mock(MoneyFormatter.class);
        JobsProperties jobs = mock(JobsProperties.class);

        when(jobs.zone()).thenReturn("UTC");
        YnabMonthResponse response = new YnabMonthResponse(
                new YnabMonthResponse.Data(
                        42L,
                        new YnabMonthResponse.Month(12345L)
                )
        );

        when(ynabClient.getMonth(anyString(), eq(5L))).thenReturn(response);

        BalanceService service =  new BalanceService(ynabClient, moneyFormatter, jobs);
        BalanceSnapshot snapshot = service.getBalanceWithKnowledge(5L);

        assertThat(snapshot.valueMilli()).isEqualTo(12345L);
        assertThat(snapshot.serverKnowledge()).isEqualTo(42L);
        verify(ynabClient).getMonth(anyString(), eq(5L));
    }

    @Test
    void formatsAvailableBalanceWithPrefix() {
        YnabClient ynabClient = mock(YnabClient.class);
        MoneyFormatter moneyFormatter = mock(MoneyFormatter.class);
        JobsProperties jobs = mock(JobsProperties.class);

        when(moneyFormatter.formatMilliunits(1000L)).thenReturn("₽1.00");

        BalanceService service = new BalanceService(ynabClient, moneyFormatter, jobs);

        assertThat(service.formatAvailableBalance(1000L))
                .isEqualTo("Доступный баланс: ₽1.00");
    }
}
