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

    public JobsController(JobsProperties jobs, BalancePollService balancePollService) {
        this.jobs = jobs;
        this.balancePollService = balancePollService;
    }

    @PostMapping("/poll")
    public ResponseEntity<Void> poll(
            @RequestHeader(name = "X-Budgetbot-Job-Token", required = false) String token
    ) {
        String expected = jobs.jobToken();

        if (expected == null || expected.isBlank()) {
            log.error("BUDGETBOT_JOB_TOKEN is not set (jobs.job-token empty).");
            return ResponseEntity.status(500).build();
        }

        if (!expected.equals(token)) {
            return ResponseEntity.status(403).build();
        }

        balancePollService.checkAndNotifyIfChanged();
        return ResponseEntity.ok().build();
    }
}