package com.inyourcode.transport.session.threads;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 异步线程池
 */
public class AsyncExecutorPool {
    private static volatile boolean start = false;
    private static ScheduledExecutorService service;

    public static void start(int corePoolSize){
        if (!start) {
            start = true;
            service = new ScheduledThreadPoolExecutor(corePoolSize, new NameThreadFactory("async"));
        }
    }

    public static void shutdown(){
        service.shutdown();
    }

    public static void execute(Runnable runnable) {
        if (!start){
            throw new RuntimeException("The thread pool is not open.");
        }
        service.execute(runnable);
    }

    public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                            long initialDelay,
                                                            long period,
                                                            TimeUnit unit) {
        if (!start){
            throw new RuntimeException("The thread pool is not open.");
        }
        return service.scheduleWithFixedDelay(command, initialDelay, period, unit);
    }

    public static void execute(Runnable runnable, long delay) {
        if (!start){
            throw new RuntimeException("The thread pool is not open.");
        }
        service.schedule(runnable, delay, TimeUnit.MILLISECONDS);
    }

}
