package com.lakhmann.budgetbot.config;

import com.lakhmann.budgetbot.config.properties.CurrencyProperties;
import com.lakhmann.budgetbot.config.properties.JobsProperties;
import com.lakhmann.budgetbot.config.properties.TelegramProperties;
import com.lakhmann.budgetbot.config.properties.YnabProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({
        YnabProperties.class,
        TelegramProperties.class,
        JobsProperties.class,
        CurrencyProperties.class
})
public class HttpClientsConfig {

    @Bean(name = "ynabRestClient")
    public RestClient ynabRestClient(RestClient.Builder builder, YnabProperties props) {
        return builder
                .baseUrl(props.baseUrl())
                .defaultHeader("Authorization", "Bearer " + props.token())
                .build();
    }

    @Bean(name = "telegramRestClient")
    public RestClient telegramRestClient(RestClient.Builder builder, TelegramProperties props) {
        return builder
                .baseUrl("%s/bot%s".formatted(props.baseUrl(), props.botToken()))
                .build();
    }
}
