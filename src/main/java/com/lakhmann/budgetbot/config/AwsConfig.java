package com.lakhmann.budgetbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.kms.KmsClient;

@Configuration
public class AwsConfig {

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(resolveRegion())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public KmsClient kmsClient() {
        return KmsClient.builder()
                .region(resolveRegion())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    private Region resolveRegion() {
        try {
            return new DefaultAwsRegionProviderChain().getRegion();
        } catch (Exception ex) {
            return Region.IL_CENTRAL_1;
        }
    }
}
