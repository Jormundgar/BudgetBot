package com.lakhmann.budgetbot.telegram.miniapp;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class OAuthStateServiceTest {

    @Test
    void issuesAndVerifiesState() {
        OAuthStateService service = new OAuthStateService("bot-token", 600);

        String state = service.issue(777L);

        assertThat(service.verifyAndExtractUserId(state)).isEqualTo(777L);
    }

    @Test
    void rejectsExpiredState() {
        OAuthStateService service = new OAuthStateService("bot-token", -1);
        String state = service.issue(123L);

        assertThatThrownBy(() -> service.verifyAndExtractUserId(state))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }
}
