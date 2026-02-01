package org.example.homedatazip.global.batch.busstation.writer;

import jakarta.persistence.EntityManagerFactory;
import org.example.homedatazip.busstation.entity.BusStation;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusStationJpaWriter {

    @Bean
    public JpaItemWriter<BusStation> busStationWriter(EntityManagerFactory emf) {
        JpaItemWriter<BusStation> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);
        return writer;
    }
}
