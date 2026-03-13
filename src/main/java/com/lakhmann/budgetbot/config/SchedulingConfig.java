package com.lakhmann.budgetbot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConditionalOnProperty(value = "jobs.scheduling.enabled", havingValue = "true")
@EnableScheduling
public class SchedulingConfig {
}
