package com.lakhmann.budgetbot.jobs;

import com.lakhmann.budgetbot.balance.BalanceService;
import com.lakhmann.budgetbot.balance.BalanceSnapshot;
import com.lakhmann.budgetbot.balance.state.BalanceState;
import com.lakhmann.budgetbot.balance.state.BalanceStateStore;
import com.lakhmann.budgetbot.telegram.TelegramNotificationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Tag("unit")
class BalancePollServiceTest {

    private final BalanceStateStore stateStore = mock(BalanceStateStore.class);
    private final BalanceService balanceService = mock(BalanceService.class);
    private final TelegramNotificationService notificationService = mock(TelegramNotificationService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final BalancePollService service =
            new BalancePollService(stateStore, balanceService, notificationService, clock);

    @Test
    void skipsWhenServerKnowledgeUnchanged() {
        BalanceState prev = new BalanceState(100L, 10L, Instant.now(clock));
        when(stateStore.load()).thenReturn(Optional.of(prev));
        when(balanceService.getBalanceWithKnowledge(10L))
                .thenReturn(new BalanceSnapshot(200L, 10L));

        service.checkAndNotifyIfChanged();

        verify(notificationService, never()).notifyAllRecipients(anyString());
        verify(stateStore, never()).save(any());
    }

    @Test
    void savesStateWhenValueSameButKnowledgeChanged() {
        BalanceState prev = new BalanceState(100L, 10L, Instant.now(clock));
        when(stateStore.load()).thenReturn(Optional.of(prev));
        when(balanceService.getBalanceWithKnowledge(10L))
                .thenReturn(new BalanceSnapshot(100L, 11L));

        service.checkAndNotifyIfChanged();

        ArgumentCaptor<BalanceState> captor = ArgumentCaptor.forClass(BalanceState.class);
        verify(stateStore).save(captor.capture());
        assertThat(captor.getValue().lastValueMilli()).isEqualTo(100L);
        assertThat(captor.getValue().lastServerKnowledge()).isEqualTo(11L);
        verify(notificationService, never()).notifyAllRecipients(anyString());
    }

    @Test
    void notifiesAndPersistsWhenValueChanges() {
        BalanceState prev = new BalanceState(100L, 10L, Instant.now(clock));
        when(stateStore.load()).thenReturn(Optional.of(prev));
        when(balanceService.getBalanceWithKnowledge(10L))
                .thenReturn(new BalanceSnapshot(200L, 11L));
        when(balanceService.formatAvailableBalance(200L)).thenReturn("Доступный баланс: ₽2.00");
        when(notificationService.notifyAllRecipients("Доступный баланс: ₽2.00"))
                .thenReturn(List.of(1L, 2L));

        service.checkAndNotifyIfChanged();

        verify(notificationService).notifyAllRecipients("Доступный баланс: ₽2.00");
        verify(stateStore).save(any(BalanceState.class));
    }
}