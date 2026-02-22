package com.lakhmann.budgetbot.jobs;

import com.lakhmann.budgetbot.balance.BalanceService;
import com.lakhmann.budgetbot.balance.state.BalanceState;
import com.lakhmann.budgetbot.balance.state.BalanceStateStore;
import com.lakhmann.budgetbot.telegram.TelegramClient;

import com.lakhmann.budgetbot.user.UserYnabConnectionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class BalancePollService {

    private static final Logger log = LoggerFactory.getLogger(BalancePollService.class);

    private final BalanceStateStore stateStore;
    private final BalanceService balanceService;
    private final TelegramClient telegramClient;
    private final Clock clock;
    private final UserYnabConnectionStore connectionStore;

    public BalancePollService(BalanceStateStore stateStore,
                              BalanceService balanceService,
                              TelegramClient telegramClient,
                              Clock clock,
                              UserYnabConnectionStore connectionStore) {
        this.stateStore = stateStore;
        this.balanceService = balanceService;
        this.telegramClient = telegramClient;
        this.clock = clock;
        this.connectionStore = connectionStore;
    }

    public void checkAndNotifyIfChanged() {
        connectionStore.listConnectedUserIds().forEach(this::processUser);
    }

    private void processUser(long userId) {
        BalanceState prev = stateStore.load(userId).orElse(null);
        Long lastSk = prev == null ? null : prev.lastServerKnowledge();

        var cur = balanceService.getBalanceWithKnowledge(userId, lastSk);
        Long serverKnowledge = cur.serverKnowledge();

        if (isServerKnowledgeUnchanged(prev, serverKnowledge)) {
            return;
        }

        Long newMilli = cur.valueMilli();
        Long oldMilli = prev == null ? null : prev.lastValueMilli();

        if (oldMilli != null && oldMilli.equals(newMilli)) {
            saveState(userId, oldMilli, serverKnowledge);
            return;
        }

        telegramClient.sendPlainMessage(userId, balanceService.formatAvailableBalance(newMilli));
        saveState(userId, newMilli, serverKnowledge);
        log.info("User {} notified: oldMilli={}, newMilli={}", userId, oldMilli, newMilli);
    }

    private boolean isServerKnowledgeUnchanged(BalanceState prev, Long serverKnowledge) {
        return prev != null
                && prev.lastServerKnowledge() != null
                && serverKnowledge != null
                && serverKnowledge.equals(prev.lastServerKnowledge());
    }

    private void saveState(long userId, Long milli, Long serverKnowledge) {
        stateStore.save(userId, new BalanceState(milli, serverKnowledge, Instant.now(clock)));
    }
}