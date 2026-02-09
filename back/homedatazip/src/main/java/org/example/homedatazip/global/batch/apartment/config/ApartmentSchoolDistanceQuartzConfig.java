package org.example.homedatazip.global.batch.apartment.config;

import org.example.homedatazip.global.batch.apartment.quartz.ApartmentSchoolDistanceQuartzJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApartmentSchoolDistanceQuartzConfig {

    @Value("${apartment.school-distance.sync.cron}")
    private String cronExpression;

    @Bean
    public JobDetail apartmentSchoolDistanceJobDetail() {
        return JobBuilder.newJob(ApartmentSchoolDistanceQuartzJob.class)
                .withIdentity("apartmentSchoolDistance")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger apartmentSchoolDistanceTrigger(JobDetail apartmentSchoolDistanceJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(apartmentSchoolDistanceJobDetail)
                .withIdentity("apartmentSchoolDistanceTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
    }
}