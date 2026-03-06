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
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Map;

public class SecretsManagerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SecretsManagerEnvironmentPostProcessor.class);
    private static final String SECRETS_ID_ENV = "BUDGETBOT_SECRETS_ID";
    private static final String PROPERTY_SOURCE_NAME = "aws-secrets-manager";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String secretId = environment.getProperty(SECRETS_ID_ENV);
        if (secretId == null || secretId.isBlank()) {
            return;
        }

        try (SecretsManagerClient client = SecretsManagerClient.create()) {
            GetSecretValueResponse resp = client.getSecretValue(
                    GetSecretValueRequest.builder().secretId(secretId).build()
            );
            String secretString = resp.secretString();
            if (secretString == null || secretString.isBlank()) {
                log.warn("Secrets Manager secret {} is empty.", secretId);
                return;
            }

            Map<String, Object> props = objectMapper.readValue(
                    secretString,
                    new TypeReference<>() {}
            );
            if (props.isEmpty()) {
                log.warn("Secrets Manager secret {} parsed as empty JSON.", secretId);
                return;
            }

            environment.getPropertySources()
                    .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
            log.info("Loaded {} properties from Secrets Manager.", props.size());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load secrets from Secrets Manager", e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
