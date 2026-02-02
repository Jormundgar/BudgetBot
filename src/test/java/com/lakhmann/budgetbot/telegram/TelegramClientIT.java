package com.lakhmann.budgetbot.telegram;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Tag("integration")
class TelegramClientIT {

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
    void sendsBottomKeyboard() {
        stubFor(post(urlEqualTo("/sendMessage"))
                .willReturn(ok()));

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
        TelegramClient client = new TelegramClient(restClient);

        client.ensureBottomKeyboard(55L);

        verify(postRequestedFor(urlEqualTo("/sendMessage"))
                .withRequestBody(matchingJsonPath("$.chat_id", equalTo("55")))
                .withRequestBody(matchingJsonPath("$.reply_markup.keyboard[0][0].text",
                        equalTo(TelegramMessages.BALANCE_COMMAND_TEXT))));
    }

    @Test
    void sendsPlainMessage() {
        stubFor(post(urlEqualTo("/sendMessage"))
                .willReturn(ok()));

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
        TelegramClient client = new TelegramClient(restClient);

        client.sendPlainMessage(11L, "hi");

        verify(postRequestedFor(urlEqualTo("/sendMessage"))
                .withRequestBody(matchingJsonPath("$.chat_id", equalTo("11")))
                .withRequestBody(matchingJsonPath("$.text", equalTo("hi"))));
    }
}