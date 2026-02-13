package org.example.homedatazip.global.quartz;

import org.quartz.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 모든 Batch Job을 Quartz로 정기 실행하는 통합 설정.
   각 Batch별로 할 일(JobDetail)과 시간(Trigger)을 정의
 */
@Configuration
public class QuartzConfig {

    public static final String SCHEDULER_CONTEXT_APP_CTX = "applicationContext";
    public static final String SCHEDULER_CONTEXT_JOB_LAUNCHER = "jobLauncher";

    /**
     * Quartz Job이 Spring Bean(JobLauncher, ApplicationContext)에 접근할 수 있도록 SchedulerContext에 넣어둔다. (JobFactory autowiring 실패 시 대비)
     */
    @Bean
    public SchedulerFactoryBeanCustomizer schedulerCustomizer(
            ApplicationContext applicationContext,
            JobLauncher jobLauncher) {
        return (factory) -> {
            AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
            jobFactory.setApplicationContext(applicationContext);
            factory.setJobFactory(jobFactory);

            // JobFactory autowiring 실패 시 대비: SchedulerContext에도 저장
            Map<String, Object> ctx = new HashMap<>();
            ctx.put(SCHEDULER_CONTEXT_APP_CTX, applicationContext);
            ctx.put(SCHEDULER_CONTEXT_JOB_LAUNCHER, jobLauncher);
            factory.setSchedulerContextAsMap(ctx);
        };
    }

    private static final String BATCH_JOB_BEAN_KEY = BatchLauncherQuartzJob.JOB_DATA_KEY_BATCH_JOB_BEAN_NAME;

    // ─── 아파트 실거래 적재 (매일 2시)
    @Value("${quartz.batch.apartment-trade.cron:0 0 2 * * ?}")
    private String apartmentTradeCron;

    // ─── 학교 OpenAPI 동기화 (매일 2시 10분)
    @Value("${school.openapi.sync.cron:0 10 2 * * ?}")
    private String schoolOpenApiSyncCron;

    // ─── 지하철역 OpenAPI 동기화 (매일 2시)
    @Value("${subway.openapi.sync.cron:0 0 2 * * ?}")
    private String subwayOpenApiSyncCron;

    // ─── 아파트-지하철 거리 (매일 3시)
    @Value("${apartment.subway-distance.sync.cron:0 0 3 * * ?}")
    private String apartmentSubwayDistanceCron;

    // ─── 아파트-학교 거리 (매일 4시)
    @Value("${apartment.school-distance.sync.cron:0 0 4 * * ?}")
    private String apartmentSchoolDistanceCron;

    // ─── Region 적재 (매주 일요일 1시 - 지역코드 변경 빈도 낮음)
    @Value("${quartz.batch.region.cron:0 0 1 ? * SUN}")
    private String regionCron;

    // ─── 버스 정류장 OpenAPI 적재 (매일 1시)
    @Value("${quartz.batch.bus-station.cron:0 0 1 * * ?}")
    private String busStationCron;

    // ─── 전월세(TradeRent) 적재 (매일 5시 - Region/아파트 이후)
    @Value("${quartz.batch.trade-rent.cron:0 0 5 * * ?}")
    private String tradeRentCron;

    // ─── 병원 데이터 적재 (매일 5시 30분)
    @Value("${quartz.batch.hospital.cron:0 30 5 * * ?}")
    private String hospitalCron;

    @Bean
    public JobDetail apartmentTradeJobDetail() {
        return JobBuilder.newJob(BatchLauncherQuartzJob.class)
                .withIdentity("apartmentTrade")
                .storeDurably()
                .usingJobData(BATCH_JOB_BEAN_KEY, "apartmentTradeJob")
                .build();
    }

    @Bean
    public Trigger apartmentTradeTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(apartmentTradeJobDetail())
                .withIdentity("apartmentTradeTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(apartmentTradeCron))
                .build();
    }

    @Bean
    public JobDetail schoolOpenApiSyncJobDetail() {
        return JobBuilder.newJob(BatchLauncherQuartzJob.class)
                .withIdentity("schoolOpenApiSync")
                .storeDurably()
                .usingJobData(BATCH_JOB_BEAN_KEY, "schoolOpenApiSyncJob")
                .build();
    }

    @Bean
    public Trigger schoolOpenApiSyncTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(schoolOpenApiSyncJobDetail())
                .withIdentity("schoolOpenApiSyncTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(schoolOpenApiSyncCron))
                .build();
    }

    @Bean
    public JobDetail subwayStationOpenApiSyncJobDetail() {
        return JobBuilder.newJob(BatchLauncherQuartzJob.class)
                .withIdentity("subwayStationOpenApiSync")
                .storeDurably()
                .usingJobData(BATCH_JOB_BEAN_KEY, "subwayStationOpenApiSyncJob")
                .build();
    }

    @Bean
    public Trigger subwayStationOpenApiSyncTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(subwayStationOpenApiSyncJobDetail())
                .withIdentity("subwayStationOpenApiSyncTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(subwayOpenApiSyncCron))
                .build();
    }

    @Bean
    public JobDetail apartmentSubwayDistanceJobDetail() {
        return JobBuilder.newJob(BatchLauncherQuartzJob.class)
                .withIdentity("apartmentSubwayDistance")
                .storeDurably()
                .usingJobData(BATCH_JOB_BEAN_KEY, "apartmentSubwayDistanceJob")
                .build();
    }

    @Bean
    public Trigger apartmentSubwayDistanceTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(apartmentSubwayDistanceJobDetail())
                .withIdentity("apartmentSubwayDistanceTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(apartmentSubwayDistanceCron))
                .build();
    }

    @Bean
    public JobDetail apartmentSchoolDistanceJobDetail() {
        return JobBuilder.newJob(BatchLauncherQuartzJob.class)
                .withIdentity("apartmentSchoolDistance")
                .storeDurably()
                .usingJobData(BATCH_JOB_BEAN_KEY, "apartmentSchoolDistanceJob")
                .build();
    }

    @Bean
    public Trigger apartmentSchoolDistanceTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(apartmentSchoolDistanceJobDetail())
                .withIdentity("apartmentSchoolDistanceTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(apartmentSchoolDistanceCron))
                .build();
    }

    @Bean
    public JobDetail regionJobDetail() {
        return JobBuilder.newJob(BatchLauncherQuartzJob.class)
                .withIdentity("region")
                .storeDurably()
                .usingJobData(BATCH_JOB_BEAN_KEY, "regionJob")
                .build();
    }

    @Bean
    public Trigger regionTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(regionJobDetail())
                .withIdentity("regionTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(regionCron))
                .build();
    }

    @Bean
    public JobDetail busStationJobDetail() {
        return JobBuilder.newJob(BatchLauncherQuartzJob.class)
                .withIdentity("busStation")
                .storeDurably()
                .usingJobData(BATCH_JOB_BEAN_KEY, "busStationJob")
                .build();
    }

    @Bean
    public Trigger busStationTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(busStationJobDetail())
                .withIdentity("busStationTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(busStationCron))
                .build();
    }

    @Bean
    public JobDetail tradeRentBackfillJobDetail() {
        return JobBuilder.newJob(BatchLauncherQuartzJob.class)
                .withIdentity("tradeRentBackfill")
                .storeDurably()
                .usingJobData(BATCH_JOB_BEAN_KEY, "tradeRentBackfillJob")
                .build();
    }

    @Bean
    public Trigger tradeRentBackfillTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(tradeRentBackfillJobDetail())
                .withIdentity("tradeRentBackfillTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(tradeRentCron))
                .build();
    }

    @Bean
    public JobDetail hospitalImportJobDetail() {
        return JobBuilder.newJob(BatchLauncherQuartzJob.class)
                .withIdentity("hospitalImport")
                .storeDurably()
                .usingJobData(BATCH_JOB_BEAN_KEY, "hospitalImportJob")
                .build();
    }

    @Bean
    public Trigger hospitalImportTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(hospitalImportJobDetail())
                .withIdentity("hospitalImportTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(hospitalCron))
                .build();
    }
}
