package com.lakhmann.budgetbot.balance.state;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("dev")
public class InMemoryBalanceStateStore implements BalanceStateStore {

    private final Map<Long, BalanceState> states = new ConcurrentHashMap<>();

    @Override
    public Optional<BalanceState> load(long userId) {
        return Optional.ofNullable(states.get(userId));
    }

    @Override
    public void save(long userId, BalanceState state) {
            states.put(userId, state);
    }
}
