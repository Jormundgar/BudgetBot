package com.lakhmann.budgetbot.telegram.miniapp;

import com.lakhmann.budgetbot.balance.BalanceService;
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
    @Value("${telegram.bot-token}")
    private String botToken;

    public MiniAppController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @PostMapping("/balance")
    public MiniAppBalanceResponse balance(@RequestBody MiniAppRequest req) {
        var vr = TelegramInitDataValidator.validate(req.initData(), botToken);
        if (!vr.valid()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid initData: " + vr.error());
        }

        var snap = balanceService.getBalanceWithKnowledge(null);

        return new MiniAppBalanceResponse(
                snap.valueMilli(),
                balanceService.formatAvailableBalance(snap.valueMilli()),
                Instant.now().toString()
        );
    }

    @PostMapping("/refresh")
    public MiniAppBalanceResponse refresh(@RequestBody MiniAppRequest req) {
        var vr = TelegramInitDataValidator.validate(req.initData(), botToken);
        if (!vr.valid()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid initData: " + vr.error());
        }

        var snap = balanceService.forceRefreshSnapshot();

        return new MiniAppBalanceResponse(
                snap.valueMilli(),
                balanceService.formatAvailableBalance(snap.valueMilli()),
                Instant.now().toString()
        );
    }
}
