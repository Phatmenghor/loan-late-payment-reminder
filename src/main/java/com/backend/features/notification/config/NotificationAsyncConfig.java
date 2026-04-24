package com.backend.features.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "app.notification")
@Getter
@Setter
public class NotificationAsyncConfig {

    private Integer batchSize = 20;
    private Integer maxThreads = 10;

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(maxThreads);
        executor.setMaxPoolSize(maxThreads);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notification-async-");
        executor.initialize();
        return executor;
    }
}

