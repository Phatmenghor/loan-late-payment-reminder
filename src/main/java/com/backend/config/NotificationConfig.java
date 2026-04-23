package com.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "notification.sms")
@Data
public class NotificationConfig {

    private boolean enabled;
    private String apiEndpoint;
    private int timeoutSeconds;

    public String getFullApiUrl(String baseUrl) {
        return baseUrl + apiEndpoint;
    }
}
