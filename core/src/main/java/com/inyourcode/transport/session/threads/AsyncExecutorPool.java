package com.inyourcode.transport.session.threads;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 异步线程池
 */
public class AsyncExecutorPool {

    private static ScheduledExecutorService service;

    public static synchronized void start(int corePoolSize){
        if(service == null) {
            service = new ScheduledThreadPoolExecutor(corePoolSize, new NameThreadFactory("async"));
        }
    }

    public static void shutdown(){
        service.shutdown();
    }

    public static void execute(Runnable runnable) {
        if(service == null){
            throw new RuntimeException("请先开启异步线程池");
        }
        service.execute(runnable);
    }

    public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                            long initialDelay,
                                                            long period,
                                                            TimeUnit unit) {
        if(service == null){
            throw new RuntimeException("请先开启异步线程池");
        }
        return service.scheduleWithFixedDelay(command, initialDelay, period, unit);
    }

    public static void execute(Runnable runnable, long delay) {
        if(service == null){
            throw new RuntimeException("请先开启异步线程池");
        }
        service.schedule(runnable, delay, TimeUnit.MILLISECONDS);
    }

}
