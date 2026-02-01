package org.example.homedatazip.global.batch.busstation.config;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse.Row;
import org.example.homedatazip.busstation.entity.BusStation;
import org.example.homedatazip.global.batch.busstation.reader.BusStationApiReader;
import org.example.homedatazip.global.batch.busstation.processor.BusStationUpsertProcessor;
import org.example.homedatazip.global.batch.busstation.tasklet.BusStationGeocodeTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BusStationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;

    private final BusStationApiReader busStationApiReader;
    private final BusStationUpsertProcessor busStationUpsertProcessor;
    private final JpaItemWriter<BusStation> busStationWriter;
    private final BusStationGeocodeTasklet busStationGeocodeTasklet;

    @Bean
    public Job busStationJob() {
        return new JobBuilder("busStationJob", jobRepository)
                .start(busStationLoadStep())
                .next(busStationGeocodeStep())
                .build();
    }

    @Bean
    public Step busStationLoadStep() {
        return new StepBuilder("busStationLoadStep", jobRepository)
                .<Row, BusStation>chunk(500, txManager)
                .reader(busStationApiReader)
                .processor(busStationUpsertProcessor)
                .writer(busStationWriter)
                .build();
    }

    @Bean
    public Step busStationGeocodeStep() {
        return new StepBuilder("busStationGeocodeStep", jobRepository)
                .tasklet(busStationGeocodeTasklet, txManager)
                .build();
    }
}
