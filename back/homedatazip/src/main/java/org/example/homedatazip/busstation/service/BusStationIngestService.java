package org.example.homedatazip.busstation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusStationIngestService {

    private final JobLauncher jobLauncher;
    private final Job busStationJob;

    public JobExecution run() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(busStationJob, params);

            log.info("[BUS_STATION] job started. jobId={}, status={}",
                    execution.getJobId(), execution.getStatus());

            return execution;
        } catch (Exception e) {
            log.error("[BUS_STATION] job run failed.", e);
            throw new IllegalStateException("busStationJob 실행 실패", e);
        }
    }
}
