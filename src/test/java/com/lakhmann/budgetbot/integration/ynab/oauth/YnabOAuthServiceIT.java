package com.lakhmann.budgetbot.integration.ynab.oauth;

import com.lakhmann.budgetbot.config.properties.YnabProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("integration")
class YnabOAuthServiceIT {

    private MockRestServiceServer server;
    private RestClient.Builder restClientBuilder;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder()
                .baseUrl("http://localhost");
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void exchangesAuthCodeAndRefreshToken() {
        YnabProperties props = new YnabProperties(
                "http://localhost",
                "http://localhost/oauth/authorize",
                "http://localhost/oauth/token",
                "client-id",
                "client-secret",
                "https://app/callback"
        );
        server.expect(ExpectedCount.times(2), requestTo("http://localhost/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token":"a1","refresh_token":"r1"}
                        """, MediaType.APPLICATION_JSON));

        YnabOAuthService service = new YnabOAuthService(restClientBuilder, props);

        YnabTokenResponse byCode = service.exchangeCode("code");
        YnabTokenResponse byRefresh = service.exchangeRefreshToken("refresh");

        assertThat(byCode.accessToken()).isEqualTo("a1");
        assertThat(byRefresh.refreshToken()).isEqualTo("r1");
        assertThat(service.buildAuthorizeUrl("state-1")).contains("state=state-1");
    }
}
