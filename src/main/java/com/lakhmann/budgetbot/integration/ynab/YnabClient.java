package com.lakhmann.budgetbot.integration.ynab;

import com.lakhmann.budgetbot.config.properties.YnabProperties;
import com.lakhmann.budgetbot.integration.ynab.dto.YnabMonthResponse;
import com.lakhmann.budgetbot.integration.ynab.dto.YnabTransactionsResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Service
public class YnabClient {

    private final RestClient ynabRestClient;
    private final YnabProperties props;

    public YnabClient(@Qualifier("ynabRestClient") RestClient ynabRestClient, YnabProperties props) {
        this.ynabRestClient = ynabRestClient;
        this.props = props;
    }

    public YnabMonthResponse getMonth(String monthParam) {
        return getMonth(monthParam, null);
    }

    public YnabMonthResponse getMonth(String monthParam, Long lastKnowledgeOfServer) {
        return ynabRestClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder.path("/budgets/{budgetId}/months/{month}");
                    if (lastKnowledgeOfServer != null) {
                        b = b.queryParam("last_knowledge_of_server", lastKnowledgeOfServer);
                    }
                    return b.build(props.budgetId(), monthParam);
                })
                .retrieve()
                .body(YnabMonthResponse.class);
    }

    public List<YnabTransactionsResponse.Transaction> getTransactionsSince(LocalDate sinceDate) {
        YnabTransactionsResponse resp = ynabRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/budgets/{budgetId}/transactions")
                        .queryParam("since_date", sinceDate)
                        .build(props.budgetId()))
                .retrieve()
                .body(YnabTransactionsResponse.class);

        if (resp == null || resp.data() == null || resp.data().transactions() == null) {
            return List.of();
        }
        return resp.data().transactions();
    }
}
