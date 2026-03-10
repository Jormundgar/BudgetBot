package com.lakhmann.budgetbot;

import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class LambdaHandlerTest {

    @Test
    void initializesHandler() {
        SpringBootLambdaContainerHandler<?, ?> handler = LambdaHandler.getHandler();
        assertThat(handler).isNotNull();
    }
}
