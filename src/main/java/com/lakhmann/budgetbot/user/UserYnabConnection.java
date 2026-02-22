package com.lakhmann.budgetbot.user;

import java.time.Instant;

public record UserYnabConnection(
        long userId,
        String refreshToken,
        String budgetId,
        Instant updatedAt
) {}

