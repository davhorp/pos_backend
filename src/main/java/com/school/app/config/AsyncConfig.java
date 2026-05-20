package com.school.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 1. Hilos que siempre estarán activos
        executor.setCorePoolSize(5);
        // 2. Máximo de hilos permitidos si la cola se llena
        executor.setMaxPoolSize(15);
        // 3. Capacidad de la cola antes de crear nuevos hilos
        executor.setQueueCapacity(500);
        // 4. Nombre identificativo en los logs (útil para debug)
        executor.setThreadNamePrefix("AuditThread-");
        // 5. Estrategia cuando todo está lleno (CallerRunsPolicy: lo ejecuta el hilo principal)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

}
