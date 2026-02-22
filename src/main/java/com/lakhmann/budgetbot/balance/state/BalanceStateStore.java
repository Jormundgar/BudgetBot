package com.lakhmann.budgetbot.balance.state;

import java.util.Optional;

public interface BalanceStateStore {
    Optional<BalanceState> load(long userId);
    void save(long userId, BalanceState state);
}
