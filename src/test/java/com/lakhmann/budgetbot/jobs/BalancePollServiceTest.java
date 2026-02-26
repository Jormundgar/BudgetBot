package com.lakhmann.budgetbot.jobs;

import com.lakhmann.budgetbot.balance.BalanceService;
import com.lakhmann.budgetbot.balance.BalanceSnapshot;
import com.lakhmann.budgetbot.balance.state.BalanceState;
import com.lakhmann.budgetbot.balance.state.BalanceStateStore;
import com.lakhmann.budgetbot.telegram.TelegramClient;
import com.lakhmann.budgetbot.user.UserYnabConnectionStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
class BalancePollServiceTest {

    private final BalanceStateStore stateStore = mock(BalanceStateStore.class);
    private final BalanceService balanceService = mock(BalanceService.class);
    private final TelegramClient telegramClient = mock(TelegramClient.class);
    private final UserYnabConnectionStore connectionStore = mock(UserYnabConnectionStore.class);
    private final Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final BalancePollService service =
            new BalancePollService(stateStore, balanceService, telegramClient, clock, connectionStore);

    @Test
    void skipsWhenServerKnowledgeUnchangedAndValueSame() {
        long userId = 101L;
        BalanceState prev = new BalanceState(100L, 10L, Instant.now(clock));
        when(connectionStore.listConnectedUserIds()).thenReturn(List.of(userId));
        when(stateStore.load(userId)).thenReturn(Optional.of(prev));
        when(balanceService.getBalanceWithKnowledge(userId, 10L)).thenReturn(new BalanceSnapshot(100L, 10L));

        service.checkAndNotifyIfChanged();

        verify(telegramClient, never()).sendPlainMessage(anyLong(), anyString());
        verify(stateStore, never()).save(eq(userId), any());
    }

    @Test
    void savesStateWhenValueSameButKnowledgeChanged() {
        long userId = 102L;
        BalanceState prev = new BalanceState(100L, 10L, Instant.now(clock));
        when(connectionStore.listConnectedUserIds()).thenReturn(List.of(userId));
        when(stateStore.load(userId)).thenReturn(Optional.of(prev));
        when(balanceService.getBalanceWithKnowledge(userId, 10L)).thenReturn(new BalanceSnapshot(100L, 11L));

        service.checkAndNotifyIfChanged();

        ArgumentCaptor<BalanceState> captor = ArgumentCaptor.forClass(BalanceState.class);
        verify(stateStore).save(eq(userId), captor.capture());
        assertThat(captor.getValue().lastValueMilli()).isEqualTo(100L);
        assertThat(captor.getValue().lastServerKnowledge()).isEqualTo(11L);
        verify(telegramClient, never()).sendPlainMessage(anyLong(), anyString());
    }

    @Test
    void notifiesAndPersistsWhenValueChanges() {
        long userId = 103L;
        BalanceState prev = new BalanceState(100L, 10L, Instant.now(clock));
        when(connectionStore.listConnectedUserIds()).thenReturn(List.of(userId));
        when(stateStore.load(userId)).thenReturn(Optional.of(prev));
        when(balanceService.getBalanceWithKnowledge(userId, 10L)).thenReturn(new BalanceSnapshot(200L, 11L));
        when(balanceService.formatAvailableBalance(200L)).thenReturn("Доступный баланс: ₽2.00");

        service.checkAndNotifyIfChanged();

        verify(telegramClient).sendPlainMessage(userId, "Доступный баланс: ₽2.00");
        verify(stateStore).save(eq(userId), any(BalanceState.class));
    }

    @Test
    void notifiesWhenValueChangesEvenIfKnowledgeUnchanged() {
        long userId = 104L;
        BalanceState prev = new BalanceState(100L, 10L, Instant.now(clock));
        when(connectionStore.listConnectedUserIds()).thenReturn(List.of(userId));
        when(stateStore.load(userId)).thenReturn(Optional.of(prev));
        when(balanceService.getBalanceWithKnowledge(userId, 10L)).thenReturn(new BalanceSnapshot(200L, 10L));
        when(balanceService.formatAvailableBalance(200L)).thenReturn("Доступный баланс: ₽2.00");

        service.checkAndNotifyIfChanged();

        verify(telegramClient).sendPlainMessage(userId, "Доступный баланс: ₽2.00");
        verify(stateStore).save(eq(userId), any(BalanceState.class));
    }
}
