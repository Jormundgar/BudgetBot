package com.lakhmann.budgetbot.jobs;

import com.lakhmann.budgetbot.config.properties.JobsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jobs")
public class JobsController {

    private static final Logger log = LoggerFactory.getLogger(JobsController.class);

    private final JobsProperties jobs;
    private final BalancePollService balancePollService;
    private final DailyBalanceJob dailyBalanceJob;

    public JobsController(JobsProperties jobs,
                          BalancePollService balancePollService,
                          DailyBalanceJob dailyBalanceJob) {
        this.jobs = jobs;
        this.balancePollService = balancePollService;
        this.dailyBalanceJob = dailyBalanceJob;
    }

    @PostMapping("/poll")
    public ResponseEntity<Void> poll(
            @RequestHeader(name = "X-Budgetbot-Job-Token", required = false) String token
    ) {
        if (!isAuthorized(token)) {
            return authErrorResponse();
        }

        balancePollService.checkAndNotifyIfChanged();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/daily")
    public ResponseEntity<Void> daily(
            @RequestHeader(name = "X-Budgetbot-Job-Token", required = false) String token
    ) {
        if (!isAuthorized(token)) {
            return authErrorResponse();
        }

        dailyBalanceJob.run();
        return ResponseEntity.ok().build();
    }

    private boolean isAuthorized(String token) {
        String expected = jobs.jobToken();
        if (expected == null || expected.isBlank()) {
            log.error("BUDGETBOT_JOB_TOKEN is not set (jobs.job-token empty).");
            return false;
        }
        return expected.equals(token);
    }

    private ResponseEntity<Void> authErrorResponse() {
        String expected = jobs.jobToken();
        if (expected == null || expected.isBlank()) {
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.status(403).build();
    }
}
