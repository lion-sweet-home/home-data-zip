package org.example.homedatazip.global.batch.school.config;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.batch.school.processor.SchoolProcessor;
import org.example.homedatazip.global.batch.school.quartz.SchoolOpenApiSyncQuartzJob;
import org.example.homedatazip.global.batch.school.reader.SchoolApiReader;
import org.example.homedatazip.school.dto.SchoolSourceSync;
import org.quartz.*;
import org.quartz.JobBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class SchoolBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Value("${school.openapi.sync.cron}")
    private String cronExpression;

    // 1. [Writer] 데이터 저장 설정
    @Bean
    public JdbcBatchItemWriter<SchoolSourceSync> schoolSourceUpsertWriter() {
        String sql = """
            INSERT INTO school_sources
                (school_id, name, school_level, road_address, jibun_address, latitude, longitude, created_at, updated_at)
            VALUES
                (:schoolId, :schoolName, :schoolLevel, :roadAddress, :jibunAddress, :latitude, :longitude, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                school_level = VALUES(school_level),
                updated_at = CURRENT_TIMESTAMP
            """;
        JdbcBatchItemWriter<SchoolSourceSync> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql(sql);
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.afterPropertiesSet();
        return writer;
    }

    // 2. [Step & Job] 배치 공정 조립
    @Bean
    public Step schoolOpenApiSyncStep(
            SchoolApiReader reader,
            SchoolProcessor processor) {
        return new StepBuilder("schoolOpenApiSyncStep", jobRepository)
                .<SchoolSourceSync, SchoolSourceSync>chunk(200, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(schoolSourceUpsertWriter())
                .build();
    }

    @Bean
    public Job schoolOpenApiSyncJob(Step schoolOpenApiSyncStep) {
        return new org.springframework.batch.core.job.builder.JobBuilder("schoolOpenApiSyncJob", jobRepository)
                .start(schoolOpenApiSyncStep)
                .build();
    }

    // 3. [Quartz] 스케줄러 설정
    @Bean
    public JobDetail schoolOpenApiSyncJobDetail() {
        return JobBuilder.newJob(SchoolOpenApiSyncQuartzJob.class)
                .withIdentity("schoolOpenApiSync")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger schoolOpenApiSyncTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(schoolOpenApiSyncJobDetail())
                .withIdentity("schoolOpenApiSyncTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
    }
}