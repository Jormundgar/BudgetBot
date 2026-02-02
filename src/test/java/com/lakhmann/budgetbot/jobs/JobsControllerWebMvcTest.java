package com.lakhmann.budgetbot.jobs;

import com.lakhmann.budgetbot.config.properties.JobsProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobsController.class)
@Tag("slice")
class JobsControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobsProperties jobsProperties;

    @MockBean
    private BalancePollService balancePollService;

    @Test
    void returnsServerErrorWhenTokenMissing() throws Exception {
        when(jobsProperties.jobToken()).thenReturn("");

        mockMvc.perform(post("/jobs/poll")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void returnsForbiddenWhenTokenMismatch() throws Exception {
        when(jobsProperties.jobToken()).thenReturn("secret");

        mockMvc.perform(post("/jobs/poll")
                        .header("X-Budgetbot-Job-Token", "wrong"))
                .andExpect(status().isForbidden());
    }

    @Test
    void triggersPollWhenTokenMatches() throws Exception {
        when(jobsProperties.jobToken()).thenReturn("secret");

        mockMvc.perform(post("/jobs/poll")
                        .header("X-Budgetbot-Job-Token", "secret"))
                .andExpect(status().isOk());

        verify(balancePollService).checkAndNotifyIfChanged();
    }
}