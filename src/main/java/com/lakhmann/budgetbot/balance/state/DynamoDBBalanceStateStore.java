package com.lakhmann.budgetbot.balance.state;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Profile("!dev")
@Component
public class DynamoDBBalanceStateStore implements BalanceStateStore {

    private final DynamoDbClient dynamo;
    private final String table;

    public DynamoDBBalanceStateStore(
            DynamoDbClient dynamo,
            @Value("${DYNAMODB_BALANCE_STATE_TABLE:}") String table) {
        this.dynamo = dynamo;
        this.table = table;
    }

    @Override
    public Optional<BalanceState> load(long userId) {

        var req = GetItemRequest.builder()
                .tableName(table)
                .key(Map.of("userId", AttributeValue.fromN(Long.toString(userId))))
                .consistentRead(true)
                .build();

        var resp = dynamo.getItem(req);
        if (!resp.hasItem() || resp.item().isEmpty()) {
            return Optional.empty();
        }

        var item = resp.item();

        Long lastValueMilli = item.containsKey("lastValueMilli") ? Long.parseLong(item.get("lastValueMilli").n()) : null;
        Long lastSk = item.containsKey("lastServerKnowledge") ? Long.parseLong(item.get("lastServerKnowledge").n()) : null;
        Instant updatedAt = item.containsKey("updatedAt") ? Instant.parse(item.get("updatedAt").s()) : Instant.EPOCH;

        return Optional.of(new BalanceState(lastValueMilli, lastSk, updatedAt));
    }

    @Override
    public void save(long userId, BalanceState state) {
        var item = new HashMap<String, AttributeValue>();
        item.put("userId", AttributeValue.fromN(Long.toString(userId)));

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
