package com.lakhmann.budgetbot.balance;

import com.lakhmann.budgetbot.config.properties.JobsProperties;
import com.lakhmann.budgetbot.integration.ynab.YnabClient;
import com.lakhmann.budgetbot.integration.ynab.dto.YnabMonthResponse;
import com.lakhmann.budgetbot.user.UserYnabAuthService;
import com.lakhmann.budgetbot.user.YnabUserSession;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
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
        UserYnabAuthService authService = mock(UserYnabAuthService.class);

        when(jobs.zone()).thenReturn("UTC");
        when(authService.sessionFor(1001L))
                .thenReturn(new YnabUserSession("access-token", "budget-id"));

        YnabMonthResponse response = new YnabMonthResponse(
                new YnabMonthResponse.Data(
                        42L,
                        new YnabMonthResponse.Month(12345L)
                )
        );

        when(ynabClient.getMonth(eq("access-token"), eq("budget-id"), anyString(), nullable(Long.class)))
                .thenReturn(response);

        BalanceService service = new BalanceService(ynabClient, moneyFormatter, jobs, authService);
        BalanceSnapshot snapshot = service.getBalanceWithKnowledge(1001L, null);

        assertThat(snapshot.valueMilli()).isEqualTo(12345L);
        assertThat(snapshot.serverKnowledge()).isEqualTo(42L);
        verify(authService).sessionFor(1001L);
        verify(ynabClient).getMonth(eq("access-token"), eq("budget-id"), anyString(), nullable(Long.class));
    }

    @Test
    void formatsAvailableBalanceWithPrefix() {
        YnabClient ynabClient = mock(YnabClient.class);
        MoneyFormatter moneyFormatter = mock(MoneyFormatter.class);
        JobsProperties jobs = mock(JobsProperties.class);
        UserYnabAuthService authService = mock(UserYnabAuthService.class);

        when(moneyFormatter.formatMilliunits(1000L)).thenReturn("₽1.00");

        BalanceService service = new BalanceService(ynabClient, moneyFormatter, jobs, authService);

        assertThat(service.formatAvailableBalance(1000L))
                .isEqualTo("Доступный баланс: ₽1.00");
    }
}
