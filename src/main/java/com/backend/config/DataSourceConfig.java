package com.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@Slf4j
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "datasource.oracle")
    public DataSource oracleDataSource() {
        log.info("Configuring SECONDARY datasource: Oracle");
        return DataSourceBuilder.create().build();
    }
}
