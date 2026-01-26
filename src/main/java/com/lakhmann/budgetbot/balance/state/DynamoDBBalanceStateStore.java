package com.lakhmann.budgetbot.balance.state;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Profile("!dev")
@Component
public class DynamoDBBalanceStateStore implements BalanceStateStore {

    private static final String PK_VALUE = "BALANCE_STATE";

    private final DynamoDbClient dynamo;
    private final String table;

    public DynamoDBBalanceStateStore(
            DynamoDbClient dynamo,
            @Value("${DYNAMODB_TABLE:}") String table) {
        this.dynamo = dynamo;
        this.table = table;
    }

    @Override
    public Optional<BalanceState> load() {
        if (table == null || table.isBlank()) {
            throw new IllegalStateException("DYNAMODB_TABLE env is not set");
        }

        var req = GetItemRequest.builder()
                .tableName(table)
                .key(Map.of("pk", AttributeValue.fromS(PK_VALUE)))
                .consistentRead(true)
                .build();

        var resp = dynamo.getItem(req);
        if (!resp.hasItem() || resp.item().isEmpty()) {
            return Optional.empty();
        }

        var item = resp.item();

        Long lastValueMilli = null;
        if (item.containsKey("lastValueMilli") && item.get("lastValueMilli").n() != null) {
            lastValueMilli = Long.parseLong(item.get("lastValueMilli").n());
        }

        Long lastSk = null;
        if (item.containsKey("lastServerKnowledge") && item.get("lastServerKnowledge").n() != null) {
            lastSk = Long.parseLong(item.get("lastServerKnowledge").n());
        }

        Instant updatedAt = Instant.EPOCH;
        if (item.containsKey("updatedAt") && item.get("updatedAt").s() != null) {
            updatedAt = Instant.parse(item.get("updatedAt").s());
        }

        return Optional.of(new BalanceState(lastValueMilli, lastSk, updatedAt));
    }

    @Override
    public void save(BalanceState state) {
        if (table == null || table.isBlank()) {
            throw new IllegalStateException("DYNAMODB_TABLE env is not set");
        }

        var item = new java.util.HashMap<String, AttributeValue>();
        item.put("pk", AttributeValue.fromS(PK_VALUE));

        if (state.lastValueMilli() != null) {
            item.put("lastValueMilli", AttributeValue.fromN(state.lastValueMilli().toString()));
        }
        if (state.lastServerKnowledge() != null) {
            item.put("lastServerKnowledge", AttributeValue.fromN(state.lastServerKnowledge().toString()));
        }
        item.put("updatedAt", AttributeValue.fromS(state.updatedAt().toString()));

        var req = PutItemRequest.builder()
                .tableName(table)
                .item(item)
                .build();

        dynamo.putItem(req);
    }
}
