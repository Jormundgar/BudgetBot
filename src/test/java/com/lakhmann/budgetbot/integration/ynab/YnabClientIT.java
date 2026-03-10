package com.lakhmann.budgetbot.integration.ynab;

import com.lakhmann.budgetbot.integration.ynab.dto.YnabMonthResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("integration")
class YnabClientIT {

    private MockRestServiceServer server;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost");
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void callsYnabMonthEndpointWithAuthAndQuery() {
        server.expect(requestTo(startsWith("http://localhost/budgets/budget-123/months/2024-01-01")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("last_knowledge_of_server", "10"))
                .andExpect(header("Authorization", "Bearer token"))
                .andRespond(withSuccess("""
                        {
                          "data": {
                            "server_knowledge": 10,
                            "month": {
                              "to_be_budgeted": 5000
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        YnabClient client = new YnabClient(restClient);

        YnabMonthResponse response = client.getMonth("token", "budget-123", "2024-01-01", 10L);

        assertThat(response.data().serverKnowledge()).isEqualTo(10L);
    }

    @Test
    void readsPrimaryBudgetIdAndTransactions() {
        server.expect(requestTo("http://localhost/budgets"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token"))
                .andRespond(withSuccess("""
                        {"data":{"budgets":[{"id":"b-1","last_used_on":"2025-01-01"}]}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(startsWith("http://localhost/budgets/b-1/transactions")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("since_date", "2025-01-01"))
                .andExpect(header("Authorization", "Bearer token"))
                .andRespond(withSuccess("""
                        {"data":{"transactions":[{"id":"t1","date":"2025-01-01","amount":1000,"deleted":false}]}}
                        """, MediaType.APPLICATION_JSON));
        YnabClient client = new YnabClient(restClient);

        String budgetId = client.getPrimaryBudgetId("token");

        assertThat(budgetId).isEqualTo("b-1");
        assertThat(client.getTransactionsSince("token", "b-1", LocalDate.parse("2025-01-01"))).hasSize(1);
    }
}
