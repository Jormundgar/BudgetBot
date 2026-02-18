package com.lakhmann.budgetbot.integration.ynab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public record YnabTransactionsResponse(Data data) {

    public record Data(
            List<Transaction> transactions
    ) {}

    public record Transaction(
            String id,
            LocalDate date,
            Long amount,
            @JsonProperty("payee_name") String payeeName,
            @JsonProperty("category_name") String categoryName,
            Boolean deleted
    ) {}
}

