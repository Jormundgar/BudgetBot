package com.lakhmann.budgetbot.integration.ynab;

import com.lakhmann.budgetbot.config.properties.YnabProperties;
import com.lakhmann.budgetbot.integration.ynab.dto.YnabMonthResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
}
