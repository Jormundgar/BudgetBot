package com.lakhmann.budgetbot.user;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryUserYnabConnectionStoreTest {

    @Test
    void savesAndReturnsConnection() {
        InMemoryUserYnabConnectionStore store = new InMemoryUserYnabConnectionStore();
        UserYnabConnection connection = new UserYnabConnection(1L, "refresh", "budget", Instant.now());

        store.save(connection);

        assertThat(store.get(1L)).contains(connection);
        assertThat(store.listConnectedUserIds()).containsExactly(1L);
    }
}
