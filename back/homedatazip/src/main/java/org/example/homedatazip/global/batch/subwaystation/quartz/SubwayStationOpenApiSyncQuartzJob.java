package org.example.homedatazip.global.batch.subwaystation.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Quartz 가 주기적으로 실행하는 Job.
 * - 실행 시 Spring Batch Job(subwayStationOpenApiSyncJob)을 JobLauncher 로 한 번 실행.
 * - "언제" 실행할지는 Trigger(cron)에서 정함.
 * - Quartz 가 인스턴스를 만들고, Spring Boot 의 AutowiringSpringBeanJobFactory 가 의존성 주입.
 */
@Slf4j
public class SubwayStationOpenApiSyncQuartzJob implements Job {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("subwayStationOpenApiSyncJob")
    private org.springframework.batch.core.Job subwayStationOpenApiSyncJob;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            var params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();
            var execution = jobLauncher.run(subwayStationOpenApiSyncJob, params);
            log.info("[Quartz] 지하철 역 OpenAPI 동기화 완료: status={}", execution.getStatus());
        } catch (Exception e) {
            log.error("[Quartz] 지하철 역 OpenAPI 동기화 실패", e);
            throw new JobExecutionException(e);
        }
    }
}
