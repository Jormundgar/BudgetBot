package com.lakhmann.budgetbot.jobs;

import com.lakhmann.budgetbot.config.properties.JobsProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobsController.class)
@TestPropertySource(properties = {
        "jobs.job-token=secret"
})
@Import(JobsControllerWebMvcTest.TestConfig.class)
@Tag("slice")
class JobsControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BalancePollService balancePollService;

    @MockBean
    private DailyBalanceJob dailyBalanceJob;

    @Test
    void returnsForbiddenWhenHeaderMissing() throws Exception {
        mockMvc.perform(post("/jobs/poll")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsForbiddenWhenTokenMismatch() throws Exception {
        mockMvc.perform(post("/jobs/poll")
                        .header("X-Budgetbot-Job-Token", "wrong"))
                .andExpect(status().isForbidden());
    }

    @Test
    void triggersPollWhenTokenMatches() throws Exception {
        mockMvc.perform(post("/jobs/poll")
                        .header("X-Budgetbot-Job-Token", "secret"))
                .andExpect(status().isOk());

        verify(balancePollService).checkAndNotifyIfChanged();
    }

    @Test
    void triggersDailyWhenTokenMatches() throws Exception {
        mockMvc.perform(post("/jobs/daily")
                        .header("X-Budgetbot-Job-Token", "secret"))
                .andExpect(status().isOk());

        verify(dailyBalanceJob).run();
    }

    @TestConfiguration
    @EnableConfigurationProperties(JobsProperties.class)
    static class TestConfig {
    }
}
