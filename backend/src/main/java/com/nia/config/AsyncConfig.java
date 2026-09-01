package com.nia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables simple backend-owned async execution for user-triggered long-running
 * work (e.g. news refresh). A small thread pool is enough — NIA deliberately
 * avoids Redis/Kafka/queues. Scheduled ingestion continues to use @Scheduled.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String OPERATION_EXECUTOR = "operationExecutor";

    @Bean(OPERATION_EXECUTOR)
    public Executor operationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("nia-op-");
        executor.initialize();
        return executor;
    }
}
