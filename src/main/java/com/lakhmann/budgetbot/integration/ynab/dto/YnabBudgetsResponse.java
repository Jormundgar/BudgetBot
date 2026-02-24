package com.lakhmann.budgetbot.integration.ynab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record YnabBudgetsResponse(Data data) {

    public record Data(List<Budget> budgets) {}

    public record Budget(
            String id,
            @JsonProperty("last_used_on") String lastUsedOn) {}
}
