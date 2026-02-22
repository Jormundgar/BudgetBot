package com.lakhmann.budgetbot.user;

import com.lakhmann.budgetbot.integration.ynab.oauth.YnabOAuthService;
import com.lakhmann.budgetbot.integration.ynab.oauth.YnabTokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class UserYnabAuthService {

    private final UserYnabConnectionStore store;
    private final YnabOAuthService oauthService;

    public UserYnabAuthService(UserYnabConnectionStore store, YnabOAuthService oauthService) {
        this.store = store;
        this.oauthService = oauthService;
    }

    public YnabUserSession sessionFor(long userId) {
        UserYnabConnection connection = store.get(userId)
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "YNAB not connected"));

        YnabTokenResponse refreshed = oauthService.exchangeRefreshToken(connection.refreshToken());
        String newRefreshToken = refreshed.refreshToken() == null ? connection.refreshToken() : refreshed.refreshToken();

        store.save(new UserYnabConnection(
                userId,
                newRefreshToken,
                connection.budgetId(),
                Instant.now()
        ));

        return new YnabUserSession(refreshed.accessToken(), connection.budgetId());
    }
}

