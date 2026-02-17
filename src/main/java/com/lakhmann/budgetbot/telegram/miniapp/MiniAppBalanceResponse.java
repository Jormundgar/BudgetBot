package com.lakhmann.budgetbot.telegram.miniapp;

public record MiniAppBalanceResponse(
        long balanceMilli,
        String balanceText,
        String updatedAt,
        Long telegramUserId
) {
}
