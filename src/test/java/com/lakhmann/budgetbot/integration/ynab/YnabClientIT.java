package com.lakhmann.budgetbot.integration.ynab;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.lakhmann.budgetbot.config.properties.YnabProperties;
import com.lakhmann.budgetbot.integration.ynab.dto.YnabMonthResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

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
    void callsYnabEndpointWithQueryParam() {
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

        YnabProperties props = new YnabProperties(
                "http://localhost:" + wireMockServer.port(),
                "token",
                "budget-123"
        );
        RestClient restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Authorization", "Bearer " + props.token())
                .build();
        YnabClient client = new YnabClient(restClient, props);

        YnabMonthResponse response = client.getMonth("2024-01-01", 10L);

        assertThat(response.data().serverKnowledge()).isEqualTo(10L);
        assertThat(response.data().month().toBeBudgetedMilliunits()).isEqualTo(5000L);
        verify(getRequestedFor(urlPathEqualTo("/budgets/budget-123/months/2024-01-01"))
                .withQueryParam("last_knowledge_of_server", equalTo("10")));
    }
}