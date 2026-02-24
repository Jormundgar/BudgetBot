package com.lakhmann.budgetbot.integration.ynab.oauth;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.lakhmann.budgetbot.config.properties.YnabProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class YnabOAuthServiceIT {

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
    void exchangesAuthCodeAndRefreshToken() {
        stubFor(post(urlEqualTo("/oauth/token"))
                .willReturn(okJson("""
                        {"access_token":"a1","refresh_token":"r1"}
                        """)));

        YnabProperties props = new YnabProperties(
                "http://localhost:" + wireMockServer.port(),
                "http://localhost:" + wireMockServer.port() + "/oauth/authorize",
                "http://localhost:" + wireMockServer.port() + "/oauth/token",
                "client-id",
                "client-secret",
                "https://app/callback"
        );
        YnabOAuthService service = new YnabOAuthService(RestClient.builder(), props);

        YnabTokenResponse byCode = service.exchangeCode("code");
        YnabTokenResponse byRefresh = service.exchangeRefreshToken("refresh");

        assertThat(byCode.accessToken()).isEqualTo("a1");
        assertThat(byRefresh.refreshToken()).isEqualTo("r1");
        assertThat(service.buildAuthorizeUrl("state-1")).contains("state=state-1");
    }
}

