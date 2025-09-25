package org.example.agentprojectjava.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync // 启用Spring的异步方法执行功能
public class AsyncConfiguration implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 配置核心线程数：即使空闲也会保留的线程数量
        executor.setCorePoolSize(10);
        // 配置最大线程数：线程池允许的最大线程数
        executor.setMaxPoolSize(50);
        // 配置队列容量：用于存放等待执行任务的无界队列（如果无界，MaxPoolSize可能不生效）
        // 建议使用有界队列，如：executor.setQueueCapacity(100);
        executor.setQueueCapacity(100);
        // 配置线程空闲后的存活时间（秒）
        executor.setKeepAliveSeconds(60);
        // 配置线程名称前缀，便于调试
        executor.setThreadNamePrefix("MyAsyncExecutor-");
        // 初始化执行器
        executor.initialize();
        return executor;
    }
}
