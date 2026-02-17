package com.lakhmann.budgetbot.telegram.miniapp;

import com.lakhmann.budgetbot.balance.BalanceService;
import com.lakhmann.budgetbot.telegram.recipients.RecipientsStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/api/miniapp")
public class MiniAppController {

    private final BalanceService balanceService;
    private final RecipientsStore  recipientsStore;
    @Value("${telegram.bot-token}")
    private String botToken;

    public MiniAppController(BalanceService balanceService,
                             RecipientsStore recipientsStore) {
        this.balanceService = balanceService;
        this.recipientsStore = recipientsStore;
    }

    @PostMapping("/balance")
    public MiniAppBalanceResponse balance(@RequestBody MiniAppRequest req) {
        var vr = TelegramInitDataValidator.validate(req.initData(), botToken);
        if (!vr.valid()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid initData: " + vr.error());
        }

        var snap = balanceService.getBalanceWithKnowledge(null);
        Long userId = TelegramInitDataValidator.extractUserId(vr);
        requireAllowed(userId);

        return new MiniAppBalanceResponse(
                snap.valueMilli(),
                balanceService.formatAvailableBalance(snap.valueMilli()),
                Instant.now().toString(),
                userId
        );
    }

    @PostMapping("/refresh")
    public MiniAppBalanceResponse refresh(@RequestBody MiniAppRequest req) {
        var vr = TelegramInitDataValidator.validate(req.initData(), botToken);
        if (!vr.valid()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid initData: " + vr.error());
        }

        var snap = balanceService.forceRefreshSnapshot();
        Long userId = TelegramInitDataValidator.extractUserId(vr);
        requireAllowed(userId);

        return new MiniAppBalanceResponse(
                snap.valueMilli(),
                balanceService.formatAvailableBalance(snap.valueMilli()),
                Instant.now().toString(),
                userId
        );
    }

    private void requireAllowed(long telegramUserId) {
        if (!recipientsStore.contains(telegramUserId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Not allowed. Send /start to the bot first."
            );
        }
    }
}
