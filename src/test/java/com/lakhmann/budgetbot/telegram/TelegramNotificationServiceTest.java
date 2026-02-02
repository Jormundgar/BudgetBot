package com.lakhmann.budgetbot.telegram;

import com.lakhmann.budgetbot.telegram.recipients.RecipientsStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Tag("unit")
class TelegramNotificationServiceTest {

    @Test
    void skipsWhenNoRecipients() {
        TelegramClient client = mock(TelegramClient.class);
        RecipientsStore store = mock(RecipientsStore.class);
        when(store.listAll()).thenReturn(List.of());

        TelegramNotificationService service = new TelegramNotificationService(client, store);

        List<Long> recipients = service.notifyAllRecipients("text");

        assertThat(recipients).isEmpty();
        verify(client, never()).sendPlainMessage(anyLong(), anyString());
    }

    @Test
    void notifiesEveryRecipient() {
        TelegramClient client = mock(TelegramClient.class);
        RecipientsStore store = mock(RecipientsStore.class);
        when(store.listAll()).thenReturn(List.of(10L, 20L));

        TelegramNotificationService service = new TelegramNotificationService(client, store);

        List<Long> recipients = service.notifyAllRecipients("hello");

        assertThat(recipients).containsExactly(10L, 20L);
        verify(client).sendPlainMessage(10L, "hello");
        verify(client).sendPlainMessage(20L, "hello");
    }
}
