/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.inyourcode.transport.rpc.executor;


import com.inyourcode.common.util.NamedThreadFactory;
import com.inyourcode.common.util.Strings;
import com.inyourcode.common.util.SystemPropertyUtil;
import com.inyourcode.common.util.disruptor.RejectedTaskPolicyWithReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.inyourcode.common.util.StackTraceUtil.stackTrace;


/**
 * Provide a {@link ThreadPoolExecutor} implementation of executor.
 * Thread pool executor factory.
 *
 * jupiter
 * executor
 *
 * @author jiachun.fjc
 */
public class ThreadPoolExecutorFactory extends AbstractExecutorFactory {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolExecutorFactory.class);

    @Override
    public Executor newExecutor(Target target) {
        return new ThreadPoolExecutor(
                coreWorks(target),
                maxWorks(target),
                120L,
                TimeUnit.SECONDS,
                workQueue(target),
                new NamedThreadFactory("processor"),
                createRejectedPolicy(target, new RejectedTaskPolicyWithReport("processor")));
    }

    private BlockingQueue<Runnable> workQueue(Target target) {
        BlockingQueue<Runnable> workQueue = null;
        WorkerQueueType queueType = queueType(target, WorkerQueueType.ARRAY_BLOCKING_QUEUE);
        int queueCapacity = queueCapacity(target);
        switch (queueType) {
            case LINKED_BLOCKING_QUEUE:
                workQueue = new LinkedBlockingQueue<>(queueCapacity);
                break;
            case ARRAY_BLOCKING_QUEUE:
                workQueue = new ArrayBlockingQueue<>(queueCapacity);
                break;
        }

        return workQueue;
    }

    private WorkerQueueType queueType(Target target, WorkerQueueType defaultType) {
        WorkerQueueType queueType = null;
        switch (target) {
            case CONSUMER:
                queueType = WorkerQueueType.parse(SystemPropertyUtil.get(CONSUMER_EXECUTOR_QUEUE_TYPE));
                break;
            case PROVIDER:
                queueType = WorkerQueueType.parse(SystemPropertyUtil.get(PROVIDER_EXECUTOR_QUEUE_TYPE));
                break;
        }

        return queueType == null ? defaultType : queueType;
    }

    private RejectedExecutionHandler createRejectedPolicy(Target target, RejectedExecutionHandler defaultHandler) {
        RejectedExecutionHandler handler = null;
        String handlerClass = null;
        switch (target) {
            case CONSUMER:
                handlerClass = SystemPropertyUtil.get(CONSUMER_THREAD_POOL_REJECTED_HANDLER);
                break;
            case PROVIDER:
                handlerClass = SystemPropertyUtil.get(PROVIDER_THREAD_POOL_REJECTED_HANDLER);
                break;
        }
        if (Strings.isNotBlank(handlerClass)) {
            try {
                Class<?> cls = Class.forName(handlerClass);
                try {
                    Constructor<?> constructor = cls.getConstructor(String.class);
                    handler = (RejectedExecutionHandler) constructor.newInstance("processor");
                } catch (NoSuchMethodException e) {
                    handler = (RejectedExecutionHandler) cls.newInstance();
                }
            } catch (Exception e) {
                logger.warn("Construct {} failed, {}.", handlerClass, stackTrace(e));
            }
        }

        return handler == null ? defaultHandler : handler;
    }

    enum WorkerQueueType {
        LINKED_BLOCKING_QUEUE,
        ARRAY_BLOCKING_QUEUE;

       static WorkerQueueType parse(String name) {
            for (WorkerQueueType type : values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }
    }
}
