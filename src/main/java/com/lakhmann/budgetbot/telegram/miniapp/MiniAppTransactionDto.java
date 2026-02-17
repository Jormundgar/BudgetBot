package com.lakhmann.budgetbot.telegram.miniapp;

public record MiniAppTransactionDto(
        String id,
        String title,
        String secondaryText,
        String amount
) {}
