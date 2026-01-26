package com.lakhmann.budgetbot.balance.state;

import java.util.Optional;

public interface BalanceStateStore {
    Optional<BalanceState> load();
    void save(BalanceState state);
}
