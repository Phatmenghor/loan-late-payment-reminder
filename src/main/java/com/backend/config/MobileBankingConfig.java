package com.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mb")
@Data
public class MobileBankingConfig {

    private String otpUrl;
    private String registerCodeUrl;
    private String secretKey;
}
