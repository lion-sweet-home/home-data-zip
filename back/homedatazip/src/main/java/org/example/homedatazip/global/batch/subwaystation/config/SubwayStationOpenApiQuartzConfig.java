package org.example.homedatazip.global.batch.subwaystation.config;

import org.example.homedatazip.global.batch.subwaystation.quartz.SubwayStationOpenApiSyncQuartzJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "batch.subway-station", name = "enabled", havingValue = "true")
public class SubwayStationOpenApiQuartzConfig {

    @Value("${subway.openapi.sync.cron}")
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
