package org.example.homedatazip.subway.batch.config;

import org.example.homedatazip.subway.batch.dto.SubwayStationSourceSync;
import org.example.homedatazip.subway.batch.processor.StationApiToSourceSyncProcessor;
import org.example.homedatazip.subway.batch.reader.StationApiReader;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * OpenAPI → subway_station_sources 동기화 Step + Job.
 * - Step1: Reader(OpenAPI) → Processor(검증) → Writer(upsert). Step2(대표 역 매핑)는 나중에 추가.
 */
@Configuration
public class SubwayStationOpenApiSyncJobConfig {

    private static final int CHUNK_SIZE = 200;

    @Bean
    public Step subwayStationOpenApiSyncStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            StationApiReader stationApiReader,
            StationApiToSourceSyncProcessor stationApiToSourceSyncProcessor,
            JdbcBatchItemWriter<SubwayStationSourceSync> subwayStationSourceUpsertWriter
    ) {
        return new StepBuilder("subwayStationOpenApiSyncStep", jobRepository)
                .<SubwayStationSourceSync, SubwayStationSourceSync>chunk(CHUNK_SIZE, transactionManager)
                .reader(stationApiReader)
                .processor(stationApiToSourceSyncProcessor)
                .writer(subwayStationSourceUpsertWriter)
                .build();
    }

    @Bean
    public Job subwayStationOpenApiSyncJob(
            JobRepository jobRepository,
            Step subwayStationOpenApiSyncStep
    ) {
        return new JobBuilder("subwayStationOpenApiSyncJob", jobRepository)
                .start(subwayStationOpenApiSyncStep)
                .build();
    }
}
