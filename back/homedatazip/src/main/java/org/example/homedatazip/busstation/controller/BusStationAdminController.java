package org.example.homedatazip.busstation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/bus-stations")
public class BusStationAdminController {

    private final JobLauncher jobLauncher;

    @Qualifier("busStationJob")
    private final Job busStationJob;

    @PostMapping("/batch/run")
    public ResponseEntity<BusStationBatchRunResponse> runBatch() throws Exception {

        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis()) // 매번 다른 파라미터로 재실행 가능
                .toJobParameters();

        JobExecution execution = jobLauncher.run(busStationJob, params);

        BusStationBatchRunResponse response = BusStationBatchRunResponse.of(
                execution.getJobId(),
                execution.getStatus().name(),
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    public record BusStationBatchRunResponse(
            Long jobId,
            String status,
            LocalDateTime requestedAt
    ) {
        public static BusStationBatchRunResponse of(Long jobId, String status, LocalDateTime requestedAt) {
            return new BusStationBatchRunResponse(jobId, status, requestedAt);
        }
    }
}
