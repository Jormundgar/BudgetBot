package com.lakhmann.budgetbot.telegram;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("integration")
class TelegramClientIT {

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
    void sendsBottomKeyboard() {
        server.expect(requestTo("http://localhost/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.chat_id").value(55))
                .andExpect(jsonPath("$.text").value(TelegramMessages.READY_MESSAGE_TEXT))
                .andRespond(withSuccess());
        TelegramClient client = new TelegramClient(restClient);

        client.ensureBottomKeyboard(55L);
    }

    @Test
    void sendsPlainMessage() {
        server.expect(requestTo("http://localhost/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.chat_id").value(11))
                .andExpect(jsonPath("$.text").value("hi"))
                .andRespond(withSuccess());
        TelegramClient client = new TelegramClient(restClient);

        client.sendPlainMessage(11L, "hi");
    }
}
