package org.example.homedatazip.tradeRent.component;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XmlConfig {
    @Bean
    public XmlMapper xmlMapper() {
        return XmlMapper.builder()
                .addModule(new ParameterNamesModule())
                .build();
    }
}
