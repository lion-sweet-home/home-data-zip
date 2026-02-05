package org.example.homedatazip.global.batch.school.quartz;

import lombok.RequiredArgsConstructor;
import org.quartz.JobExecutionContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchoolOpenApiSyncQuartzJob extends QuartzJobBean {

    private final JobLauncher jobLauncher;
    private final Job schoolOpenApiSyncJob; // SchoolBatchConfig에서 만든 Job을 가져옵니다.

    @Override
    protected void executeInternal(JobExecutionContext context) {
        // 배치가 실행될 때마다 고유한 파라미터(시간)를 주어 중복 실행을 방지합니다.
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        try {
            // 실제로 배치를 실행하는 순간
            jobLauncher.run(schoolOpenApiSyncJob, jobParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}