package com.lakhmann.budgetbot.telegram.recipients;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("dev")
public class InMemoryRecipientsStore implements RecipientsStore {

    private final Set<Long> chatIds = ConcurrentHashMap.newKeySet();
    public InMemoryRecipientsStore() {
        chatIds.add(224218835L);
    }

    @Override
    public void add(long chatId) {
        chatIds.add(chatId);
    }

    @Override
    public List<Long> listAll() {
        return chatIds.stream().sorted().toList();
    }

    @Override
    public boolean contains(long chatId) {
        return chatIds.contains(chatId);
    }
}