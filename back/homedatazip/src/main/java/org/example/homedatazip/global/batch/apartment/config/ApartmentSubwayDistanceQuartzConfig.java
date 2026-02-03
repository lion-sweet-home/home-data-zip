package org.example.homedatazip.global.batch.apartment.config;

import org.example.homedatazip.global.batch.apartment.quartz.ApartmentSubwayDistanceQuartzJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApartmentSubwayDistanceQuartzConfig {

    @Value("${apartment.subway-distance.sync.cron}")
    private String cronExpression;

    @Bean
    public JobDetail apartmentSubwayDistanceJobDetail() {
        return JobBuilder.newJob(ApartmentSubwayDistanceQuartzJob.class)
                .withIdentity("apartmentSubwayDistance")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger apartmentSubwayDistanceTrigger(JobDetail apartmentSubwayDistanceJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(apartmentSubwayDistanceJobDetail)
                .withIdentity("apartmentSubwayDistanceTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
    }
}
