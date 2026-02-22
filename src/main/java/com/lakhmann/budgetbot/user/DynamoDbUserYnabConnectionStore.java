package com.lakhmann.budgetbot.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Profile("!dev")
public class DynamoDbUserYnabConnectionStore implements UserYnabConnectionStore {

    private final DynamoDbClient dynamo;
    private final String table;
    private final KmsCryptoService kmsCryptoService;
    private final Clock clock;

    public DynamoDbUserYnabConnectionStore(DynamoDbClient dynamo,
                                           @Value("${DYNAMODB_YNAB_CONNECTIONS_TABLE}") String table,
                                           KmsCryptoService kmsCryptoService,
                                           Clock clock) {
        this.dynamo = dynamo;
        this.table = table;
        this.kmsCryptoService = kmsCryptoService;
        this.clock = clock;
    }

    @Override
    public void save(UserYnabConnection connection) {
        var item = Map.of(
                "userId", AttributeValue.fromN(Long.toString(connection.userId())),
                "refreshToken", AttributeValue.fromS(kmsCryptoService.encrypt(connection.refreshToken())),
                "budgetId", AttributeValue.fromS(connection.budgetId()),
                "updatedAt", AttributeValue.fromS(Instant.now(clock).toString())
        );
        dynamo.putItem(PutItemRequest.builder().tableName(table).item(item).build());
    }

    @Override
    public Optional<UserYnabConnection> get(long userId) {
        var resp = dynamo.getItem(GetItemRequest.builder()
                .tableName(table)
                .key(Map.of("userId", AttributeValue.fromN(Long.toString(userId))))
                .consistentRead(true)
                .build());
        if (!resp.hasItem() || resp.item().isEmpty()) {
            return Optional.empty();
        }
        var item = resp.item();
        return Optional.of(new UserYnabConnection(
                userId,
                kmsCryptoService.decrypt(item.get("refreshToken").s()),
                item.get("budgetId").s(),
                Instant.parse(item.get("updatedAt").s())
        ));
    }

    @Override
    public List<Long> listConnectedUserIds() {
        var resp = dynamo.scan(ScanRequest.builder()
                .tableName(table)
                .projectionExpression("userId")
                .build());
        if (!resp.hasItems()) return List.of();
        return resp.items().stream().map(i -> Long.parseLong(i.get("userId").n())).toList();
    }
}

