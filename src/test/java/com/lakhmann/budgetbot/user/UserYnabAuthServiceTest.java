package com.lakhmann.budgetbot.user;

import com.lakhmann.budgetbot.integration.ynab.oauth.YnabOAuthService;
import com.lakhmann.budgetbot.integration.ynab.oauth.YnabTokenResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
class UserYnabAuthServiceTest {

    @Test
    void refreshesSessionAndPersistsToken() {
        UserYnabConnectionStore store = mock(UserYnabConnectionStore.class);
        YnabOAuthService oauthService = mock(YnabOAuthService.class);
        when(store.get(7L)).thenReturn(Optional.of(new UserYnabConnection(7L, "old-refresh", "budget-1", Instant.now())));
        when(oauthService.exchangeRefreshToken("old-refresh")).thenReturn(new YnabTokenResponse("access", "new-refresh"));

        UserYnabAuthService service = new UserYnabAuthService(store, oauthService);

        YnabUserSession session = service.sessionFor(7L);

        assertThat(session.accessToken()).isEqualTo("access");
        assertThat(session.budgetId()).isEqualTo("budget-1");
        verify(store).save(any(UserYnabConnection.class));
    }

    @Test
    void throwsForbiddenWhenNoConnection() {
        UserYnabConnectionStore store = mock(UserYnabConnectionStore.class);
        YnabOAuthService oauthService = mock(YnabOAuthService.class);
        when(store.get(9L)).thenReturn(Optional.empty());

        UserYnabAuthService service = new UserYnabAuthService(store, oauthService);

        assertThatThrownBy(() -> service.sessionFor(9L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }
}
