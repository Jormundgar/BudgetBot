package com.lakhmann.budgetbot.integration.ynab;

import com.lakhmann.budgetbot.integration.ynab.dto.YnabMonthResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("unit")
class YnabClientTest {

    private MockRestServiceServer server;
    private YnabClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new YnabClient(builder.build());
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void getMonthWithoutLastKnowledgeDoesNotSendQueryParam() {
        server.expect(requestTo("http://localhost/budgets/budget-123/months/2024-02-01"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andRespond(withSuccess("""
                        {"data":{"server_knowledge":1,"month":{"to_be_budgeted":5000}}}
                        """, MediaType.APPLICATION_JSON));

        YnabMonthResponse response = client.getMonth("token", "budget-123", "2024-02-01", null);

        assertThat(response.data().serverKnowledge()).isEqualTo(1L);
    }

    @Test
    void getMonthWithLastKnowledgeAddsQueryParam() {
        server.expect(requestTo(startsWith("http://localhost/budgets/budget-123/months/2024-03-01")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("last_knowledge_of_server", "42"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andRespond(withSuccess("""
                        {"data":{"server_knowledge":42,"month":{"to_be_budgeted":100}}}
                        """, MediaType.APPLICATION_JSON));

        YnabMonthResponse response = client.getMonth("token", "budget-123", "2024-03-01", 42L);

        assertThat(response.data().serverKnowledge()).isEqualTo(42L);
    }

    @Test
    void getPrimaryBudgetIdThrowsWhenNoBudgets() {
        server.expect(requestTo("http://localhost/budgets"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andRespond(withSuccess("""
                        {"data":{"budgets":[]}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getPrimaryBudgetId("token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No YNAB budgets");
    }

    @Test
    void getTransactionsSinceReturnsEmptyWhenDataMissing() {
        server.expect(requestTo(containsString("/budgets/b-1/transactions")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("since_date", "2025-01-01"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andRespond(withSuccess("""
                        {"data":{}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.getTransactionsSince("token", "b-1", LocalDate.parse("2025-01-01")))
                .isEmpty();
    }

    @Test
    void getTransactionsSinceReturnsTransactions() {
        server.expect(requestTo(containsString("/budgets/b-1/transactions")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("since_date", "2025-01-01"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andRespond(withSuccess("""
                        {"data":{"transactions":[{"id":"t1","date":"2025-01-01","amount":1000,"deleted":false}]}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.getTransactionsSince("token", "b-1", LocalDate.parse("2025-01-01")))
                .hasSize(1);
    }
}
