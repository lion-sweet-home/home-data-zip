package org.example.homedatazip.global.batch.subwaystation.config;

import javax.sql.DataSource;

import org.example.homedatazip.subway.dto.SubwayStationSourceSync;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI → subway_station_sources upsert.
 * - UNIQUE: line_station_code (lineStationCode)
 * - station_id 는 Step2(대표 역 매핑)에서 설정
 */
@Configuration
public class SubwayStationSourceWriterConfig {

    @Bean
    public JdbcBatchItemWriter<SubwayStationSourceSync> subwayStationSourceUpsertWriter(DataSource dataSource) {
        String sql = """
            INSERT INTO subway_station_sources
                (line_station_code, station_name, line_name, latitude, longitude, created_at, updated_at)
            VALUES
                (:lineStationCode, :stationName, :lineName, :latitude, :longitude, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                station_name = VALUES(station_name),
                line_name    = VALUES(line_name),
                latitude     = VALUES(latitude),
                longitude    = VALUES(longitude),
                updated_at   = CURRENT_TIMESTAMP
            """;

        JdbcBatchItemWriter<SubwayStationSourceSync> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource);
        writer.setSql(sql);
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.afterPropertiesSet();
        return writer;
    }
}
