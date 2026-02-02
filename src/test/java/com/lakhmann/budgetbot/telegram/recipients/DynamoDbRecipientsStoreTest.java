package com.lakhmann.budgetbot.telegram.recipients;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Tag("unit")
class DynamoDbRecipientsStoreTest {

    @Test
    void addStoresChatIdAndTimestamp() {
        DynamoDbClient dynamo = mock(DynamoDbClient.class);
        Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        DynamoDbRecipientsStore store = new DynamoDbRecipientsStore(dynamo, "recipients", clock);

        store.add(123L);

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamo).putItem(captor.capture());
        PutItemRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo("recipients");
        Map<String, AttributeValue> item = request.item();
        assertThat(item.get("chatId").n()).isEqualTo("123");
        assertThat(item.get("createdAt").s()).isEqualTo("2024-01-01T00:00:00Z");
    }

    @Test
    void listAllReturnsChatIds() {
        DynamoDbClient dynamo = mock(DynamoDbClient.class);
        DynamoDbRecipientsStore store = new DynamoDbRecipientsStore(dynamo, "recipients", Clock.systemUTC());

        ScanResponse response = ScanResponse.builder()
                .items(
                        Map.of("chatId", AttributeValue.fromN("1")),
                        Map.of("chatId", AttributeValue.fromN("2"))
                )
                .build();
        when(dynamo.scan(any(ScanRequest.class))).thenReturn(response);

        assertThat(store.listAll()).containsExactly(1L, 2L);
    }
}