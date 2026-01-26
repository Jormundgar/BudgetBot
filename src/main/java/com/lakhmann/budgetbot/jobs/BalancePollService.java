package com.lakhmann.budgetbot.jobs;

import com.lakhmann.budgetbot.balance.BalanceService;
import com.lakhmann.budgetbot.balance.MoneyFormatter;
import com.lakhmann.budgetbot.balance.state.BalanceState;
import com.lakhmann.budgetbot.balance.state.BalanceStateStore;
import com.lakhmann.budgetbot.telegram.TelegramClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class BalancePollService {

    private static final Logger log = LoggerFactory.getLogger(BalancePollService.class);

    private final BalanceStateStore stateStore;
    private final BalanceService balanceService;
    private final MoneyFormatter moneyFormatter;
    private final TelegramClient telegramClient;
    private final com.lakhmann.budgetbot.telegram.recipients.RecipientsStore recipientsStore;

    public BalancePollService(BalanceStateStore stateStore,
                              BalanceService balanceService,
                              MoneyFormatter moneyFormatter,
                              TelegramClient telegramClient,
                              com.lakhmann.budgetbot.telegram.recipients.RecipientsStore recipientsStore) {
        this.stateStore = stateStore;
        this.balanceService = balanceService;
        this.moneyFormatter = moneyFormatter;
        this.telegramClient = telegramClient;
        this.recipientsStore = recipientsStore;
    }

    public void checkAndNotifyIfChanged() {
        BalanceState prev = stateStore.load().orElse(null);
        Long lastSk = prev == null ? null : prev.lastServerKnowledge();

        var cur = balanceService.getBalanceWithKnowledge(lastSk);

        if (prev != null && prev.lastServerKnowledge() != null && cur.serverKnowledge() != null
                && cur.serverKnowledge().equals(prev.lastServerKnowledge())) {
            log.info("No change: server_knowledge={}", cur.serverKnowledge());
            return;
        }

        Long newMilli = cur.valueMilli();
        Long oldMilli = prev == null ? null : prev.lastValueMilli();

        if (oldMilli != null && oldMilli.equals(newMilli)) {
            stateStore.save(new BalanceState(oldMilli, cur.serverKnowledge(), Instant.now()));
            log.info("Value same (milli={}): state updated only", newMilli);
            return;
        }

        String text = "Доступный баланс: " + moneyFormatter.formatMilliunits(newMilli);

        var recipients = recipientsStore.listAll();
        if (recipients.isEmpty()) {
            log.warn("No recipients in DynamoDB, skipping notify");
        } else {
            for (Long chatId : recipients) {
                telegramClient.sendPlainMessage(chatId, text);
            }
        }

        stateStore.save(new BalanceState(newMilli, cur.serverKnowledge(), Instant.now()));
        log.info("Notified. oldMilli={}, newMilli={}, server_knowledge={}, recipients={}",
                oldMilli, newMilli, cur.serverKnowledge(), recipients.size());
    }
}