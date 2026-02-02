package com.lakhmann.budgetbot.balance.state;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@Tag("unit")
class DynamoDBBalanceStateStoreTest {

    @Test
    void loadThrowsWhenTableMissing() {
        DynamoDbClient dynamo = mock(DynamoDbClient.class);
        DynamoDBBalanceStateStore store = new DynamoDBBalanceStateStore(dynamo, " ");

        assertThatThrownBy(store::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DYNAMODB_TABLE");
    }

    @Test
    void loadReturnsParsedState() {
        DynamoDbClient dynamo = mock(DynamoDbClient.class);
        DynamoDBBalanceStateStore store = new DynamoDBBalanceStateStore(dynamo, "balance");

        Map<String, AttributeValue> item = Map.of(
                "pk", AttributeValue.fromS("BALANCE_STATE"),
                "lastValueMilli", AttributeValue.fromN("100"),
                "lastServerKnowledge", AttributeValue.fromN("12"),
                "updatedAt", AttributeValue.fromS("2024-01-01T00:00:00Z")
        );
        when(dynamo.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().item(item).build());

        Optional<BalanceState> state = store.load();

        assertThat(state).isPresent();
        assertThat(state.get().lastValueMilli()).isEqualTo(100L);
        assertThat(state.get().lastServerKnowledge()).isEqualTo(12L);
        assertThat(state.get().updatedAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Test
    void saveWritesAllFields() {
        DynamoDbClient dynamo = mock(DynamoDbClient.class);
        DynamoDBBalanceStateStore store = new DynamoDBBalanceStateStore(dynamo, "balance");

        BalanceState state = new BalanceState(200L, 99L, Instant.parse("2024-01-02T00:00:00Z"));
        store.save(state);

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamo).putItem(captor.capture());
        Map<String, AttributeValue> item = captor.getValue().item();
        assertThat(item.get("lastValueMilli").n()).isEqualTo("200");
        assertThat(item.get("lastServerKnowledge").n()).isEqualTo("99");
        assertThat(item.get("updatedAt").s()).isEqualTo("2024-01-02T00:00:00Z");
    }
}
