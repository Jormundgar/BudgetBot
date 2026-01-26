package com.lakhmann.budgetbot.integration.ynab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record YnabMonthResponse(Data data) {

    public record Data(
            @JsonProperty("server_knowledge") Long serverKnowledge,
            Month month
    ) {}

    public record Month(
            @JsonProperty("to_be_budgeted") Long toBeBudgetedMilliunits
    ) {}
}
