package com.lakhmann.budgetbot.integration.ynab;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.lakhmann.budgetbot.integration.ynab.dto.YnabMonthResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class YnabClientIT {

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void callsYnabMonthEndpointWithAuthAndQuery() {
        stubFor(get(urlPathEqualTo("/budgets/budget-123/months/2024-01-01"))
                .withQueryParam("last_knowledge_of_server", equalTo("10"))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(okJson("""
                        {
                          "data": {
                            "server_knowledge": 10,
                            "month": {
                              "to_be_budgeted": 5000
                            }
                          }
                        }
                        """)));

        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .build();
        YnabClient client = new YnabClient(restClient);

        YnabMonthResponse response = client.getMonth("token", "budget-123", "2024-01-01", 10L);

        assertThat(response.data().serverKnowledge()).isEqualTo(10L);
        verify(getRequestedFor(urlPathEqualTo("/budgets/budget-123/months/2024-01-01"))
                .withQueryParam("last_knowledge_of_server", equalTo("10")));
    }

    @Test
    void readsPrimaryBudgetIdAndTransactions() {
        stubFor(get(urlPathEqualTo("/budgets"))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(okJson("""
                        {"data":{"budgets":[{"id":"b-1","last_used_on":"2025-01-01"}]}}
                        """)));

        stubFor(get(urlPathEqualTo("/budgets/b-1/transactions"))
                .withQueryParam("since_date", equalTo("2025-01-01"))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(okJson("""
                        {"data":{"transactions":[{"id":"t1","date":"2025-01-01","amount":1000,"deleted":false}]}}
                        """)));

        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .build();
        YnabClient client = new YnabClient(restClient);

        String budgetId = client.getPrimaryBudgetId("token");

        assertThat(budgetId).isEqualTo("b-1");
        assertThat(client.getTransactionsSince("token", "b-1", LocalDate.parse("2025-01-01"))).hasSize(1);
    }
}