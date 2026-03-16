package com.sciinov.dbms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "bulkTaskExecutor")
    public Executor bulkTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);    // was 4 — allow more concurrent uploads
        executor.setMaxPoolSize(16);    // was 8
        executor.setQueueCapacity(200); // was 100
        executor.setThreadNamePrefix("bulk-upload-");
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // Set TaskDecorator to propagate SecurityContext to async threads
        executor.setTaskDecorator(new SecurityContextAwareTaskDecorator());

        executor.initialize();
        return executor;
    }

    /**
     * Dedicated executor for SSE push operations.
     * Keeps SSE broadcasts on a separate thread pool so they NEVER block
     * the upload request thread or interfere with Tomcat response handling.
     */
    @Bean(name = "ssePushExecutor")
    public Executor ssePushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("sse-push-");
        executor.setKeepAliveSeconds(30);
        executor.setWaitForTasksToCompleteOnShutdown(false); // SSE pushes are fire-and-forget
        executor.initialize();
        return executor;
    }

    /**
     * Custom TaskDecorator that preserves SecurityContext across async task execution.
     * This ensures that when @Async methods run in thread pool, they retain
     * the authentication/authorization context from the original request thread.
     */
    public static class SecurityContextAwareTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // Capture the SecurityContext from the current thread
            SecurityContext securityContext = SecurityContextHolder.getContext();

            return () -> {
                try {
                    // Set the captured SecurityContext in the async thread
                    SecurityContextHolder.setContext(securityContext);
                    runnable.run();
                } finally {
                    // Clean up SecurityContext to prevent memory leaks
                    SecurityContextHolder.clearContext();
                }
            };
        }
    }
}
