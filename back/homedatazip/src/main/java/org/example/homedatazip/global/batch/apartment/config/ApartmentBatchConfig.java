package org.example.homedatazip.global.batch.apartment.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.data.repository.RegionRepository;
import org.example.homedatazip.global.batch.apartment.partition.ApartmentIdPartitioner;
import org.example.homedatazip.global.batch.apartment.processor.ApartmentSaleItemProcessor;
import org.example.homedatazip.global.batch.apartment.reader.ApartmentSaleApiReader;
import org.example.homedatazip.global.exception.BatchRetryException;
import org.example.homedatazip.global.exception.BatchSkipException;
import org.example.homedatazip.tradeSale.dto.ApartmentTradeSaleItem;
import org.example.homedatazip.tradeSale.service.ApartmentTradeSaleService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApartmentBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RegionRepository regionRepository;
    private final ApartmentSaleApiReader apartmentSaleApiReader;
    private final ApartmentSaleItemProcessor apartmentSaleItemProcessor;
    private final ApartmentTradeSaleService apartmentTradeSaleService;
    private final FixedBackOffPolicy fixedBackOffPolicy;

    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setThreadNamePrefix("apt-batch-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Job apartmentTradeJob() {
        return new JobBuilder("apartmentTradeJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(apartmentTradeManagerStep())
                .build();
    }

    @Bean
    public Step apartmentTradeManagerStep() {
        return new StepBuilder("apartmentTradeManagerStep", jobRepository)
                .partitioner("workerStep", new ApartmentIdPartitioner(regionRepository))
                .step(apartmentTradeSaleStep())
                .gridSize(3)
                .taskExecutor(batchTaskExecutor())
                .build();
    }

    @Bean
    public Step apartmentTradeSaleStep() {
        return new StepBuilder("apartmentTradeSaleStep", jobRepository)
                .<ApartmentTradeSaleItem, ApartmentTradeSaleItem>chunk(100, transactionManager)
                .reader(apartmentSaleApiReader)
                .processor(apartmentSaleItemProcessor)
                .writer(apartmentSaleItemWriter())
                .faultTolerant() // 내결함성 기능 활성화
                .skip(BatchSkipException.class)
                .retry(BatchRetryException.class)
                .retry(org.springframework.dao.DeadlockLoserDataAccessException.class)
                .retry(org.springframework.dao.CannotAcquireLockException.class)
                .retryLimit(10) // 3번까지 다시 시도
                .backOffPolicy(fixedBackOffPolicy) // BackOff 설정 : 재시도 사이의 대기시간
                .build();
    }

    @Bean
    public ItemWriter<ApartmentTradeSaleItem> apartmentSaleItemWriter() {
        return chunk -> {
            List<ApartmentTradeSaleItem> items = new ArrayList<>(chunk.getItems());
            log.info(">>>> Writer에 넘어온 아이템 개수: {}", items.size());
            apartmentTradeSaleService.processChunk(items);

        };
    }

}
