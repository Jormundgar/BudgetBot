package com.lakhmann.budgetbot.jobs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.lakhmann.budgetbot.config.properties.JobsProperties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
class JobsControllerWebMvcTest {

    private MockMvc mockMvc(JobsProperties jobsProperties,
                            BalancePollService balancePollService,
                            DailyBalanceJob dailyBalanceJob) {
        return MockMvcBuilders.standaloneSetup(
                new JobsController(jobsProperties, balancePollService, dailyBalanceJob)
        ).build();
    }

    @Test
    void returnsServerErrorWhenTokenMissing() throws Exception {
        JobsProperties jobsProperties = new JobsProperties("0 0 7 * * *", "UTC", "");
        BalancePollService balancePollService = mock(BalancePollService.class);
        DailyBalanceJob dailyBalanceJob = mock(DailyBalanceJob.class);

        mockMvc(jobsProperties, balancePollService, dailyBalanceJob)
                .perform(post("/jobs/poll")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void returnsForbiddenWhenTokenMismatchForPoll() throws Exception {
        JobsProperties jobsProperties = new JobsProperties("0 0 7 * * *", "UTC", "secret");
        BalancePollService balancePollService = mock(BalancePollService.class);
        DailyBalanceJob dailyBalanceJob = mock(DailyBalanceJob.class);

        mockMvc(jobsProperties, balancePollService, dailyBalanceJob)
                .perform(post("/jobs/poll")
                        .header("X-Budgetbot-Job-Token", "wrong"))
                .andExpect(status().isForbidden());
    }

    @Test
    void triggersPollWhenTokenMatches() throws Exception {
        JobsProperties jobsProperties = new JobsProperties("0 0 7 * * *", "UTC", "secret");
        BalancePollService balancePollService = mock(BalancePollService.class);
        DailyBalanceJob dailyBalanceJob = mock(DailyBalanceJob.class);

        mockMvc(jobsProperties, balancePollService, dailyBalanceJob)
                .perform(post("/jobs/poll")
                        .header("X-Budgetbot-Job-Token", "secret"))
                .andExpect(status().isOk());

        verify(balancePollService).checkAndNotifyIfChanged();
    }

    @Test
    void returnsServerErrorWhenTokenMissingForDaily() throws Exception {
        JobsProperties jobsProperties = new JobsProperties("0 0 7 * * *", "UTC", "");
        BalancePollService balancePollService = mock(BalancePollService.class);
        DailyBalanceJob dailyBalanceJob = mock(DailyBalanceJob.class);

        mockMvc(jobsProperties, balancePollService, dailyBalanceJob)
                .perform(post("/jobs/daily")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void returnsForbiddenWhenTokenMismatchForDaily() throws Exception {
        JobsProperties jobsProperties = new JobsProperties("0 0 7 * * *", "UTC", "secret");
        BalancePollService balancePollService = mock(BalancePollService.class);
        DailyBalanceJob dailyBalanceJob = mock(DailyBalanceJob.class);

        mockMvc(jobsProperties, balancePollService, dailyBalanceJob)
                .perform(post("/jobs/daily")
                        .header("X-Budgetbot-Job-Token", "wrong"))
                .andExpect(status().isForbidden());
    }

    @Test
    void triggersDailyWhenTokenMatches() throws Exception {
        JobsProperties jobsProperties = new JobsProperties("0 0 7 * * *", "UTC", "secret");
        BalancePollService balancePollService = mock(BalancePollService.class);
        DailyBalanceJob dailyBalanceJob = mock(DailyBalanceJob.class);

        mockMvc(jobsProperties, balancePollService, dailyBalanceJob)
                .perform(post("/jobs/daily")
                        .header("X-Budgetbot-Job-Token", "secret"))
                .andExpect(status().isOk());

        verify(dailyBalanceJob).run();
    }
}
