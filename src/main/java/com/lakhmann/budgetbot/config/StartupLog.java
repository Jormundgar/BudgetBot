package com.lakhmann.budgetbot.config;

import com.lakhmann.budgetbot.config.properties.JobsProperties;
import com.lakhmann.budgetbot.config.properties.TelegramProperties;
import com.lakhmann.budgetbot.config.properties.YnabProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartupLog {

    private static final Logger log = LoggerFactory.getLogger(StartupLog.class);

    @Bean
    ApplicationRunner logConfig(YnabProperties ynab, TelegramProperties tg, JobsProperties jobs) {
        return args -> {
            log.info("YNAB baseUrl={}, budgetId={}", ynab.baseUrl(), ynab.budgetId());
            log.info("Telegram baseUrl={}, recipientsCount={}", tg.baseUrl(), tg.recipientIds().size());
            log.info("Jobs cron={}, zone={}", jobs.cron(), jobs.zone());
        };
    }
}
