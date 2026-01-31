package org.example.homedatazip.global.batch.hospital.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/hospital")
public class HospitalBatchController {

    private final JobLauncher jobLauncher;
    private final Job hospitalImportJob;

    /**
     * 병원 데이터 수집 배치 실행
     */
    @PostMapping("/batch/run")
    public ResponseEntity<String> runBatch() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            log.info("병원 데이터 수집 배치 시작");

            jobLauncher.run(hospitalImportJob, params);

            return ResponseEntity.ok("배치 작업 시작");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("배치 실행 실패: " + e.getMessage());
        }
    }
}
