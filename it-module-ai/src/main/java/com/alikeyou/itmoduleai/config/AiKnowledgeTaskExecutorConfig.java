package com.alikeyou.itmoduleai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AiKnowledgeTaskExecutorConfig {

    public static final String AI_KNOWLEDGE_TASK_EXECUTOR = "aiKnowledgeTaskExecutor";

    @Bean(name = AI_KNOWLEDGE_TASK_EXECUTOR)
    public Executor aiKnowledgeTaskExecutor() {
        int availableCores = Math.max(1, Runtime.getRuntime().availableProcessors());
        int corePoolSize = Math.max(2, Math.min(8, availableCores));
        int maxPoolSize = Math.max(corePoolSize, Math.min(16, availableCores * 2));

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ai-kb-task-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(300);
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
