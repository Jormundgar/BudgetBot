package com.lakhmann.budgetbot.telegram.miniapp;

import com.lakhmann.budgetbot.balance.BalanceService;
import com.lakhmann.budgetbot.integration.ynab.YnabClient;
import com.lakhmann.budgetbot.integration.ynab.oauth.YnabOAuthService;
import com.lakhmann.budgetbot.telegram.recipients.RecipientsStore;
import com.lakhmann.budgetbot.user.UserYnabConnection;
import com.lakhmann.budgetbot.user.UserYnabConnectionStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/miniapp")
public class MiniAppController {

    private final BalanceService balanceService;
    private final RecipientsStore recipientsStore;
    private final MiniAppService miniAppService;
    private final OAuthStateService stateService;
    private final YnabOAuthService oauthService;
    private final UserYnabConnectionStore connectionStore;
    private final YnabClient ynabClient;

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.miniapp-url:}")
    private String miniappUrl;

    public MiniAppController(BalanceService balanceService,
                             RecipientsStore recipientsStore,
                             MiniAppService miniAppService,
                             OAuthStateService stateService,
                             YnabOAuthService oauthService,
                             UserYnabConnectionStore connectionStore,
                             YnabClient ynabClient) {
        this.balanceService = balanceService;
        this.recipientsStore = recipientsStore;
        this.miniAppService = miniAppService;
        this.stateService = stateService;
        this.oauthService = oauthService;
        this.connectionStore = connectionStore;
        this.ynabClient = ynabClient;
    }

    @PostMapping("/oauth/start")
    public Map<String, String> oauthStart(@RequestBody MiniAppRequest req) {
        Long userId = validatedUserId(req.initData());
        requireAllowed(userId);
        return Map.of("url", oauthService.buildAuthorizeUrl(stateService.issue(userId)));
    }

    @GetMapping("/oauth/callback")
    public ResponseEntity<?> oauthCallback(@RequestParam String code, @RequestParam String state) {
        long userId = stateService.verifyAndExtractUserId(state);
        var token = oauthService.exchangeCode(code);
        String budgetId = ynabClient.getPrimaryBudgetId(token.accessToken());

        connectionStore.save(new UserYnabConnection(userId, token.refreshToken(), budgetId, Instant.now()));
        if (miniappUrl == null || miniappUrl.isBlank()) {
            return ResponseEntity.ok(Map.of("status", "connected"));
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", miniappUrl)
                .build();
    }

    @PostMapping("/balance")
    public MiniAppBalanceResponse balance(@RequestBody MiniAppRequest req) {
        Long userId = validatedUserId(req.initData());
        requireAllowed(userId);

        var snap = balanceService.getBalanceWithKnowledge(userId, null);

        return new MiniAppBalanceResponse(
                snap.valueMilli(),
                balanceService.formatAvailableBalance(snap.valueMilli()),
                Instant.now().toString(),
                userId
        );
    }

    @PostMapping("/refresh")
    public MiniAppBalanceResponse refresh(@RequestBody MiniAppRequest req) {
        Long userId = validatedUserId(req.initData());
        requireAllowed(userId);

        var snap = balanceService.forceRefreshSnapshot(userId);
        return new MiniAppBalanceResponse(
                snap.valueMilli(),
                balanceService.formatAvailableBalance(snap.valueMilli()),
                Instant.now().toString(),
                userId
        );
    }

    @PostMapping("/transactions")
    public List<MiniAppTransactionDto> lastTransactions(@RequestBody MiniAppRequest req) {
        Long userId = validatedUserId(req.initData());
        requireAllowed(userId);
        return miniAppService.lastSixTransactions(userId);
    }

    private Long validatedUserId(String initData) {
        var vr = TelegramInitDataValidator.validate(initData, botToken);
        if (!vr.valid()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid initData: " + vr.error());
        }
        return TelegramInitDataValidator.extractUserId(vr);
    }

    private void requireAllowed(long telegramUserId) {
        if (!recipientsStore.contains(telegramUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Not allowed. Send /start to the bot first."
            );
        }
    }
}
