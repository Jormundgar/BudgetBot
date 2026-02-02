package com.lakhmann.budgetbot;

import com.lakhmann.budgetbot.balance.state.BalanceStateStore;
import com.lakhmann.budgetbot.telegram.recipients.RecipientsStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SpringBootTest(properties = {
        "ynab.base-url=http://localhost",
        "ynab.token=test-token",
        "ynab.budget-id=test-budget",
        "telegram.base-url=http://localhost",
        "telegram.bot-token=test-bot",
        "telegram.webhook-secret=test-secret",
        "currency.symbol=₽",
        "currency.fraction-digits=2",
        "jobs.cron=* * * * * *",
        "jobs.zone=UTC",
        "jobs.job-token=test-token",
        "DYNAMODB_TABLE=test",
        "DYNAMODB_RECIPIENTS_TABLE=test"
})
@Tag("integration")
class BudgetbotApplicationIT {

    @MockBean
    private BalanceStateStore balanceStateStore;

    @MockBean
    private RecipientsStore recipientsStore;

    @MockBean
    private DynamoDbClient dynamoDbClient;

    @Test
    void contextLoads() {
    }

}
