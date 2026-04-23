package com.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cpb.api")
@Data
public class CpbApiConfig {

    private String url;
    private String encryptionKey;
    private int connectTimeoutMs;
    private int readTimeoutMs;
    private String username;
    private String password;

    public CpbApiConfig() {
        this.connectTimeoutMs = 5000;
        this.readTimeoutMs = 10000;
    }
}
