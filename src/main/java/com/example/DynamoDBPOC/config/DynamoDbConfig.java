package com.example.DynamoDBPOC.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.example.DynamoDBPOC.record.RecordItem;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDbConfig {

    @Bean
    DynamoDbClient dynamoDbClient(DynamoDbProperties properties) {
        var credentialsProvider = StringUtils.hasText(properties.getProfile())
            ? ProfileCredentialsProvider.create(properties.getProfile())
            : DefaultCredentialsProvider.create();

        var builder = DynamoDbClient.builder()
            .region(Region.of(properties.getRegion()))
            .credentialsProvider(credentialsProvider);

        if (StringUtils.hasText(properties.getEndpoint())) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        return builder.build();
    }

    @Bean
    DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }

    @Bean
    DynamoDbTable<RecordItem> recordTable(DynamoDbEnhancedClient enhancedClient, DynamoDbProperties properties) {
        return enhancedClient.table(properties.getTableName(), TableSchema.fromBean(RecordItem.class));
    }
}

