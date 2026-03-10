package com.lakhmann.budgetbot.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class SecretsManagerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SecretsManagerEnvironmentPostProcessor.class);
    private static final String SECRET_ARN_ENV = "BUDGETBOT_SECRET_ARN";
    private static final String SECRET_ARN_PROP = "budgetbot.secret.arn";
    private static final String SECRETS_ENABLED_PROP = "budgetbot.secrets.enabled";
    private static final String PROPERTY_SOURCE_NAME = "awsSecretsManager";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static Supplier<SecretsManagerClient> clientSupplier = SecretsManagerEnvironmentPostProcessor::defaultClient;

    private static final Map<String, String> KEY_MAP = Map.of(
            "TELEGRAM_BOT_TOKEN", "telegram.bot-token",
            "TELEGRAM_WEBHOOK_SECRET", "telegram.webhook-secret",
            "TELEGRAM_MINIAPP_URL", "telegram.miniapp-url",
            "YNAB_CLIENT_ID", "ynab.client-id",
            "YNAB_CLIENT_SECRET", "ynab.client-secret",
            "YNAB_REDIRECT_URI", "ynab.redirect-uri",
            "BUDGETBOT_JOB_TOKEN", "jobs.job-token",
            "KMS_KEY_ID", "KMS_KEY_ID"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!Boolean.parseBoolean(environment.getProperty(SECRETS_ENABLED_PROP, "true"))) {
            return;
        }
        String secretArn = resolveSecretArn(environment);
        if (secretArn == null || secretArn.isBlank()) {
            return;
        }

        Map<String, Object> props = loadSecrets(secretArn);
        if (!props.isEmpty() && environment.getPropertySources().get(PROPERTY_SOURCE_NAME) == null) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
        }
    }

    private Map<String, Object> loadSecrets(String secretArn) {
        try (SecretsManagerClient client = Objects.requireNonNull(clientSupplier.get(), "SecretsManagerClient")) {

            String secretString = client.getSecretValue(GetSecretValueRequest.builder()
                    .secretId(secretArn)
                    .build()).secretString();

            if (secretString == null || secretString.isBlank()) {
                throw new IllegalStateException("Secret is empty: " + secretArn);
            }

            Map<String, Object> raw = OBJECT_MAPPER.readValue(secretString, new TypeReference<>() {});
            Map<String, Object> mapped = new HashMap<>();

            for (var entry : raw.entrySet()) {
                String targetKey = KEY_MAP.getOrDefault(entry.getKey(), entry.getKey());
                Object value = entry.getValue();
                if (value != null) {
                    mapped.put(targetKey, value.toString());
                }
            }

            log.info("Loaded {} secret keys from Secrets Manager.", mapped.size());
            return mapped;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load Secrets Manager secret: " + secretArn, e);
        }
    }

    private String resolveSecretArn(ConfigurableEnvironment environment) {
        String secretArn = System.getenv(SECRET_ARN_ENV);
        if (secretArn == null || secretArn.isBlank()) {
            secretArn = environment.getProperty(SECRET_ARN_ENV);
        }
        if (secretArn == null || secretArn.isBlank()) {
            secretArn = environment.getProperty(SECRET_ARN_PROP);
        }
        return secretArn;
    }

    static void setClientSupplier(Supplier<SecretsManagerClient> supplier) {
        clientSupplier = supplier;
    }

    static void resetClientSupplier() {
        clientSupplier = SecretsManagerEnvironmentPostProcessor::defaultClient;
    }

    private static SecretsManagerClient defaultClient() {
        return SecretsManagerClient.builder()
                .region(resolveRegion())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    private static Region resolveRegion() {
        try {
            return new DefaultAwsRegionProviderChain().getRegion();
        } catch (Exception ex) {
            return Region.IL_CENTRAL_1;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
