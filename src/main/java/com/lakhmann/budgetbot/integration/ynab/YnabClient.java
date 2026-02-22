package com.lakhmann.budgetbot.integration.ynab;

import com.lakhmann.budgetbot.integration.ynab.dto.YnabBudgetsResponse;
import com.lakhmann.budgetbot.integration.ynab.dto.YnabMonthResponse;
import com.lakhmann.budgetbot.integration.ynab.dto.YnabTransactionsResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Service
public class YnabClient {

    private final RestClient ynabRestClient;

    public YnabClient(@Qualifier("ynabRestClient") RestClient ynabRestClient) {
        this.ynabRestClient = ynabRestClient;
    }

    public YnabMonthResponse getMonth(
            String accessToken,
            String budgetId,
            String monthParam,
            Long lastKnowledgeOfServer) {
        return ynabRestClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder.path("/budgets/{budgetId}/months/{month}");
                    if (lastKnowledgeOfServer != null) {
                        b = b.queryParam("last_knowledge_of_server", lastKnowledgeOfServer);
                    }
                    return b.build(budgetId, monthParam);
                })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(YnabMonthResponse.class);
    }

    public String getPrimaryBudgetId(String accessToken) {
        YnabBudgetsResponse response = ynabRestClient.get()
                .uri("/budgets")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(YnabBudgetsResponse.class);

        if (response == null || response.data() == null || response.data().budgets() == null || response.data().budgets().isEmpty()) {
            throw new IllegalStateException("No YNAB budgets available for user");
        }
        return response.data().budgets().get(0).id();
    }

    public List<YnabTransactionsResponse.Transaction> getTransactionsSince(
            String accessToken,
            String budgetId,
            LocalDate sinceDate) {
        YnabTransactionsResponse resp = ynabRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/budgets/{budgetId}/transactions")
                        .queryParam("since_date", sinceDate)
                        .build(budgetId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(YnabTransactionsResponse.class);

        if (resp == null || resp.data() == null || resp.data().transactions() == null) {
            return List.of();
        }
        return resp.data().transactions();
    }
}
