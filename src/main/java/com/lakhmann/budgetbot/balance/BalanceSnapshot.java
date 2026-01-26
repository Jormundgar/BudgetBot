package com.lakhmann.budgetbot.balance;

public record BalanceSnapshot(
        Long valueMilli,
        Long serverKnowledge
) {}
