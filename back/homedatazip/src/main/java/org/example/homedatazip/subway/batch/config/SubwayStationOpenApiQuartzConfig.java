package org.example.homedatazip.subway.batch.config;

import org.example.homedatazip.subway.batch.quartz.SubwayStationOpenApiSyncQuartzJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SubwayStationOpenApiQuartzConfig {

    // cron 예: "0 0 2 * * ?" = 매일 02:00:00
    @Value("${subway.openapi.sync.cron:0 0 2 * * ?}")
    private String cronExpression;

    @Bean
    public JobDetail subwayStationOpenApiSyncJobDetail() {
        return JobBuilder.newJob(SubwayStationOpenApiSyncQuartzJob.class)
                .withIdentity("subwayStationOpenApiSync")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger subwayStationOpenApiSyncTrigger(JobDetail subwayStationOpenApiSyncJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(subwayStationOpenApiSyncJobDetail)
                .withIdentity("subwayStationOpenApiSyncTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
    }
}
