package com.lakhmann.budgetbot.integration.ynab.oauth;

import com.lakhmann.budgetbot.config.properties.YnabProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("unit")
class YnabOAuthServiceTest {

    private MockRestServiceServer server;
    private YnabOAuthService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        YnabProperties props = new YnabProperties(
                "https://api.ynab.com/v1",
                "https://app.ynab.com/oauth/authorize",
                "http://localhost/oauth/token",
                "client-123",
                "secret-456",
                "http://localhost/callback"
        );
        service = new YnabOAuthService(builder, props);
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void buildAuthorizeUrlIncludesRequiredParams() {
        String url = service.buildAuthorizeUrl("state-xyz");

        var params = UriComponentsBuilder.fromUriString(url).build().getQueryParams();
        assertThat(params.getFirst("client_id")).isEqualTo("client-123");
        assertThat(params.getFirst("redirect_uri")).isEqualTo("http://localhost/callback");
        assertThat(params.getFirst("response_type")).isEqualTo("code");
        assertThat(params.getFirst("state")).isEqualTo("state-xyz");
    }

    @Test
    void exchangeCodePostsFormAndParsesTokens() {
        server.expect(requestTo("http://localhost/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .andExpect(content().string(allOf(
                        containsString("grant_type=authorization_code"),
                        containsString("code=code-123"),
                        containsString("redirect_uri=http%3A%2F%2Flocalhost%2Fcallback"),
                        containsString("client_id=client-123"),
                        containsString("client_secret=secret-456")
                )))
                .andRespond(withSuccess("""
                        {"access_token":"a-token","refresh_token":"r-token"}
                        """, MediaType.APPLICATION_JSON));

        YnabTokenResponse response = service.exchangeCode("code-123");

        assertThat(response.accessToken()).isEqualTo("a-token");
        assertThat(response.refreshToken()).isEqualTo("r-token");
    }

    @Test
    void exchangeRefreshTokenPostsFormAndParsesTokens() {
        server.expect(requestTo("http://localhost/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .andExpect(content().string(allOf(
                        containsString("grant_type=refresh_token"),
                        containsString("refresh_token=refresh-123"),
                        containsString("client_id=client-123"),
                        containsString("client_secret=secret-456")
                )))
                .andRespond(withSuccess("""
                        {"access_token":"new-a","refresh_token":"new-r"}
                        """, MediaType.APPLICATION_JSON));

        YnabTokenResponse response = service.exchangeRefreshToken("refresh-123");

        assertThat(response.accessToken()).isEqualTo("new-a");
        assertThat(response.refreshToken()).isEqualTo("new-r");
    }
}
