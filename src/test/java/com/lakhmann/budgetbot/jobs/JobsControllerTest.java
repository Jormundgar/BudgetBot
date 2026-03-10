package com.lakhmann.budgetbot.jobs;

import com.lakhmann.budgetbot.config.properties.JobsProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Tag("unit")
class JobsControllerTest {

    @Test
    void returnsServerErrorWhenJobTokenMissing() {
        JobsProperties jobsProperties = new JobsProperties("0 0 7 * * *", "UTC", "");
        BalancePollService balancePollService = mock(BalancePollService.class);
        DailyBalanceJob dailyBalanceJob = mock(DailyBalanceJob.class);

        JobsController controller = new JobsController(jobsProperties, balancePollService, dailyBalanceJob);

        assertThat(controller.poll(null).getStatusCode().value()).isEqualTo(500);
        assertThat(controller.daily(null).getStatusCode().value()).isEqualTo(500);
    }
}
