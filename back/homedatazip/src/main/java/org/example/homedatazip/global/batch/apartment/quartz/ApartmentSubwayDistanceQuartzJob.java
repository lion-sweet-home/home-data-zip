package org.example.homedatazip.global.batch.apartment.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Quartz 로 주기 실행하는 Job.
 * Spring Batch apartmentSubwayDistanceJob 을 run.id 와 함께 한 번 실행.
 */
@Slf4j
public class ApartmentSubwayDistanceQuartzJob implements Job {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("apartmentSubwayDistanceJob")
    private org.springframework.batch.core.Job apartmentSubwayDistanceJob;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            var params = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();
            var execution = jobLauncher.run(apartmentSubwayDistanceJob, params);
            log.info("[Quartz] 아파트–지하철 거리 적재 완료: status={}", execution.getStatus());
        } catch (Exception e) {
            log.error("[Quartz] 아파트–지하철 거리 적재 실패", e);
            throw new JobExecutionException(e);
        }
    }
}
