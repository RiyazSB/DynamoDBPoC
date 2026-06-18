package com.example.DynamoDBPOC.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@Configuration
public class DynamoDbTableInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDbTableInitializer.class);

    @Bean
    @ConditionalOnProperty(prefix = "aws.dynamodb", name = "auto-create-table", havingValue = "true")
    ApplicationRunner createTableIfMissing(DynamoDbClient dynamoDbClient, DynamoDbProperties properties) {
        return args -> {
            String tableName = properties.getTableName();
            try {
                dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
                LOGGER.info("DynamoDB table '{}' already exists", tableName);
            } catch (ResourceNotFoundException ex) {
                try {
                    dynamoDbClient.createTable(CreateTableRequest.builder()
                        .tableName(tableName)
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("id")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                        .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
                        .build());
                    LOGGER.info("DynamoDB table '{}' was created", tableName);
                } catch (ResourceInUseException ignored) {
                    LOGGER.info("DynamoDB table '{}' was created by another process", tableName);
                }
            } catch (DynamoDbException ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("not authorized")) {
                    LOGGER.warn("DynamoDB table '{}': Access denied. IAM user does not have DynamoDB permissions. " +
                        "Please add 'AmazonDynamoDBFullAccess' or 'dynamodb:*' policy to your IAM user. " +
                        "You can create the table manually in AWS Console and the app will use it.", tableName);
                    return;
                }
                throw ex;
            }
        };
    }
}
