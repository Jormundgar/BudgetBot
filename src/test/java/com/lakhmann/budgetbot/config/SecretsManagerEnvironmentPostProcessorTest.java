package com.lakhmann.budgetbot.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
class SecretsManagerEnvironmentPostProcessorTest {

    @AfterEach
    void tearDown() {
        SecretsManagerEnvironmentPostProcessor.resetClientSupplier();
    }

    @Test
    void doesNothingWhenSecretArnMissing() {
        ConfigurableEnvironment env = new MockEnvironment();
        var processor = new SecretsManagerEnvironmentPostProcessor();

        processor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getPropertySources().get("awsSecretsManager")).isNull();
    }

    @Test
    void loadsSecretsAndMapsKeys() {
        ConfigurableEnvironment env = new MockEnvironment()
                .withProperty("budgetbot.secret.arn", "arn:aws:secretsmanager:il-central-1:111111111111:secret:test");

        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(GetSecretValueRequest.class)))
                .thenReturn(GetSecretValueResponse.builder()
                        .secretString("""
                                {
                                  "TELEGRAM_BOT_TOKEN": "bot-token",
                                  "TELEGRAM_WEBHOOK_SECRET": "hook-secret",
                                  "YNAB_CLIENT_ID": "ynab-id",
                                  "BUDGETBOT_JOB_TOKEN": "job-token",
                                  "KMS_KEY_ID": "kms-key"
                                }
                                """)
                        .build());

        SecretsManagerEnvironmentPostProcessor.setClientSupplier(() -> client);

        var processor = new SecretsManagerEnvironmentPostProcessor();
        processor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("telegram.bot-token")).isEqualTo("bot-token");
        assertThat(env.getProperty("telegram.webhook-secret")).isEqualTo("hook-secret");
        assertThat(env.getProperty("ynab.client-id")).isEqualTo("ynab-id");
        assertThat(env.getProperty("jobs.job-token")).isEqualTo("job-token");
        assertThat(env.getProperty("KMS_KEY_ID")).isEqualTo("kms-key");
    }

    @Test
    void skipsLoadingWhenDisabledByProperty() {
        ConfigurableEnvironment env = new MockEnvironment()
                .withProperty("budgetbot.secrets.enabled", "false")
                .withProperty("budgetbot.secret.arn", "arn:aws:secretsmanager:il-central-1:111111111111:secret:test");

        var processor = new SecretsManagerEnvironmentPostProcessor();
        processor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getPropertySources().get("awsSecretsManager")).isNull();
    }
}
