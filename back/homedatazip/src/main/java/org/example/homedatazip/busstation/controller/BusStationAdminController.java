package org.example.homedatazip.busstation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.busstation.service.BusStationIngestService;
import org.springframework.batch.core.JobExecution;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/bus-stations")
public class BusStationAdminController {

    private final BusStationIngestService busStationIngestService;

    @PostMapping("/batch/run")
    public ResponseEntity<BusStationBatchRunResponse> runBatch() {

        JobExecution execution = busStationIngestService.run();

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
