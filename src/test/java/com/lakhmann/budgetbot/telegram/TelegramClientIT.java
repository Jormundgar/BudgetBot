package com.lakhmann.budgetbot.telegram;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.lakhmann.budgetbot.config.properties.TelegramProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("integration")
class TelegramClientIT {

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void sendsBottomKeyboard() {
        wireMockServer.stubFor(post(urlEqualTo("/sendMessage"))
                .willReturn(ok()));

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
        TelegramProperties props = mock(TelegramProperties.class);
        when(props.miniappUrl()).thenReturn("https://mini.app");

        TelegramClient client = new TelegramClient(restClient, props);

        client.ensureBottomKeyboard(55L);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/sendMessage"))
                .withRequestBody(matchingJsonPath("$.chat_id", equalTo("55")))
                .withRequestBody(matchingJsonPath("$.text", equalTo(TelegramMessages.READY_MESSAGE_TEXT)))
                .withRequestBody(matchingJsonPath("$.reply_markup.keyboard[0][0].text", equalTo("Открыть Mini App")))
                .withRequestBody(matchingJsonPath("$.reply_markup.keyboard[0][0].web_app.url", equalTo("https://mini.app"))));
    }

    @Test
    void sendsPlainMessage() {
        wireMockServer.stubFor(post(urlEqualTo("/sendMessage"))
                .willReturn(ok()));

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
        TelegramProperties props = mock(TelegramProperties.class);
        TelegramClient client = new TelegramClient(restClient, props);

        client.sendPlainMessage(11L, "hi");

        wireMockServer.verify(postRequestedFor(urlEqualTo("/sendMessage"))
                .withRequestBody(matchingJsonPath("$.chat_id", equalTo("11")))
                .withRequestBody(matchingJsonPath("$.text", equalTo("hi"))));
    }
}