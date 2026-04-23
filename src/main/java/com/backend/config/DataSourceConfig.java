package com.backend.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Slf4j
public class DataSourceConfig {

    @Component
    @ConfigurationProperties(prefix = "datasource.oracle")
    @Data
    public static class OracleDataSourceProperties {
        private boolean enabled;
        private String url;
        private String username;
        private String password;
        private String driverClassName;
    }
}
