package com.lakhmann.budgetbot.jobs;

import com.lakhmann.budgetbot.balance.BalanceService;
import com.lakhmann.budgetbot.balance.state.BalanceState;
import com.lakhmann.budgetbot.balance.state.BalanceStateStore;
import com.lakhmann.budgetbot.telegram.TelegramClient;
import com.lakhmann.budgetbot.telegram.recipients.RecipientsStore;

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
    private final RecipientsStore recipientsStore;
    private final Clock clock;

    public BalancePollService(BalanceStateStore stateStore,
                              BalanceService balanceService,
                              TelegramClient telegramClient,
                              RecipientsStore recipientsStore,
                              Clock clock) {
        this.stateStore = stateStore;
        this.balanceService = balanceService;
        this.telegramClient = telegramClient;
        this.recipientsStore = recipientsStore;
        this.clock = clock;
    }

    public void checkAndNotifyIfChanged() {
        BalanceState prev = loadPreviousState();
        Long lastSk = prev == null ? null : prev.lastServerKnowledge();

        var cur = balanceService.getBalanceWithKnowledge(lastSk);
        Long serverKnowledge = cur.serverKnowledge();

        if (isServerKnowledgeUnchanged(prev, serverKnowledge)) {
            log.info("No change: server_knowledge={}", serverKnowledge);
            return;
        }

        Long newMilli = cur.valueMilli();
        Long oldMilli = prev == null ? null : prev.lastValueMilli();

        if (oldMilli != null && oldMilli.equals(newMilli)) {
            saveState(oldMilli, serverKnowledge);
            log.info("Value same (milli={}): state updated only", newMilli);
            return;
        }

        String text = balanceService.formatAvailableBalance(newMilli);

        List<Long> recipients = recipientsStore.listAll();
        if (recipients.isEmpty()) {
            log.warn("No recipients in DynamoDB, skipping notify");
        } else {
            notifyRecipients(recipients, text);
        }

        saveState(newMilli, serverKnowledge);
        log.info("Notified. oldMilli={}, newMilli={}, server_knowledge={}, recipients={}",
                oldMilli, newMilli, serverKnowledge, recipients.size());
    }

    private BalanceState loadPreviousState() {
        return stateStore.load().orElse(null);
    }

    private boolean isServerKnowledgeUnchanged(BalanceState prev, Long serverKnowledge) {
        return prev != null
                && prev.lastServerKnowledge() != null
                && serverKnowledge != null
                && serverKnowledge.equals(prev.lastServerKnowledge());
    }

    private void notifyRecipients(List<Long> recipients, String text) {
        for (Long chatID : recipients) {
            telegramClient.sendPlainMessage(chatID, text);
        }
    }

    private void saveState(Long milli, Long serverKnowledge) {
        stateStore.save(new BalanceState(milli, serverKnowledge, Instant.now(clock)));
    }
}