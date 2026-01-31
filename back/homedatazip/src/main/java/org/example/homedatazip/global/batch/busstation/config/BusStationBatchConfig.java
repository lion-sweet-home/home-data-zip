package org.example.homedatazip.global.batch.busstation.config;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.example.homedatazip.busstation.service.BusStationIngestService;
import org.example.homedatazip.global.batch.busstation.processor.BusStationProcessor;
import org.example.homedatazip.global.batch.busstation.reader.BusStationApiReader;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class BusStationBatchConfig {

    private final BusStationApiReader reader;
    private final BusStationProcessor processor;
    private final BusStationIngestService ingestService;

    @Bean
    public Job busStationJob(Step busStationStep, JobRepository jobRepository) {
        return new JobBuilder("busStationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(busStationStep)
                .build();
    }

    @Bean
    public Step busStationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("busStationStep", jobRepository)
                .<SeoulBusStopResponse.Row, SeoulBusStopResponse.Row>chunk(500, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(busStationWriter())
                .build();
    }

    @Bean
    public ItemWriter<SeoulBusStopResponse.Row> busStationWriter() {
        return chunk -> {
            List<SeoulBusStopResponse.Row> rows = new ArrayList<>(chunk.getItems());
            ingestService.upsertRowsWithoutRegion(rows);
        };
    }
}
