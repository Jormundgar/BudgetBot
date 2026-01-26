package com.lakhmann.budgetbot.balance.state;

import java.time.Instant;

public record BalanceState(
        Long lastValueMilli,
        Long lastServerKnowledge,
        Instant updatedAt
) {}
