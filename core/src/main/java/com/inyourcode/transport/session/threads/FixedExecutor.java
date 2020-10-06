package com.inyourcode.transport.session.threads;


import io.netty.util.concurrent.DefaultEventExecutor;

/**
 * <u>固定的线程池
 * <li>支持根据hash值，将任务投递到指定的线程处理</li>
 *
 * </u>
 *
 * @author JackLei
 **/
public class FixedExecutor {

    private static DefaultEventExecutor[] executors;

    /**
     * 启动
     */
    public static synchronized void start(int coreSize) {
        if (executors == null) {
            executors = new DefaultEventExecutor[coreSize];
            for (int i = 0; i < coreSize; i++) {
                executors[i] = new DefaultEventExecutor(new NameThreadFactory("Fixed"));
            }
        }
    }

    /**
     * 关闭
     */
    public static void shutdownGracefully() {
        for (DefaultEventExecutor executor : executors) {
            executor.shutdownGracefully();
        }
    }

    /**
     * 执行一个任务
     *
     * @param hash
     * @param task
     */
    public static void execute(int hash, Runnable task) {
        executors[(hash % executors.length) >>> 1].execute(task);
    }

    /**
     * 执行一个任务
     *
     * @param task
     */
    public static void execute(Runnable task) {
        executors[0].execute(task);
    }
}
