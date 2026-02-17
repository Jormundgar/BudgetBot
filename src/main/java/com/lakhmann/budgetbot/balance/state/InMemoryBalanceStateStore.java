package com.lakhmann.budgetbot.balance.state;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Profile("dev")
public class InMemoryBalanceStateStore implements BalanceStateStore {

    private final AtomicReference<BalanceState> stateRef = new AtomicReference<>();

    @Override
    public Optional<BalanceState> load() {
        return Optional.ofNullable(stateRef.get());
    }

    @Override
    public void save(BalanceState state) {
        stateRef.set(state);
    }
}
