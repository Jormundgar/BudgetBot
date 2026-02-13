package com.lakhmann.budgetbot.telegram.recipients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@Profile("!dev")
public class DynamoDbRecipientsStore implements RecipientsStore {

    private final DynamoDbClient dynamo;
    private final String table;
    private final Clock clock;

    public DynamoDbRecipientsStore(
            DynamoDbClient dynamo,
            @Value("${DYNAMODB_RECIPIENTS_TABLE}") String table,
            Clock clock
    ) {
        this.dynamo = dynamo;
        this.table = table;
        this.clock = clock;
    }

    @Override
    public void add(long chatId) {
        Map<String, AttributeValue> item = Map.of(
                "chatId", AttributeValue.fromN(Long.toString(chatId)),
                "createdAt", AttributeValue.fromS(Instant.now(clock).toString())
        );

        dynamo.putItem(PutItemRequest.builder()
                .tableName(table)
                .item(item)
                .build());
    }

    @Override
    public List<Long> listAll() {
        ScanResponse resp = dynamo.scan(
                ScanRequest.builder()
                        .tableName(table)
                        .projectionExpression("chatId")
                        .build()
        );

        if (!resp.hasItems()) return List.of();

        return resp.items().stream()
                .map(i -> Long.parseLong(i.get("chatId").n()))
                .toList();
    }
}
