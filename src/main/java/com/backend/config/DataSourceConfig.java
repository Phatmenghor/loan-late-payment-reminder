package com.backend.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Configuration
@Slf4j
public class DataSourceConfig {

    @Bean(name = "oracleDataSource")
    @ConditionalOnProperty(prefix = "datasource.oracle", name = "url")
    public DataSource oracleDataSource(OracleDataSourceProperties props) {
        log.info("Configuring SECONDARY datasource: Oracle");
        return DataSourceBuilder.create()
                .url(props.url)
                .username(props.username)
                .password(props.password)
                .driverClassName(props.driverClassName)
                .build();
    }

    @Component
    @ConfigurationProperties(prefix = "datasource.oracle")
    @Data
    public static class OracleDataSourceProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
    }
}
