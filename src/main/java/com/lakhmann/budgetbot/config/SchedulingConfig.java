package com.lakhmann.budgetbot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "jobs.internal-schedule-enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
