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

import com.inyourcode.common.util.disruptor.InternalForkJoinWorkerThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provide a {@link ForkJoinPool} implementation of executor.
 *
 * jupiter
 * executor
 *
 * @author jiachun.fjc
 */
public class ForkJoinPoolExecutorFactory extends AbstractExecutorFactory {

    private static final Logger logger = LoggerFactory.getLogger(ForkJoinPoolExecutorFactory.class);

    @Override
    public Executor newExecutor(Target target) {
        return new ForkJoinPool(
                coreWorks(target),
                new DefaultForkJoinWorkerThreadFactory("fjp.processor"),
                new DefaultUncaughtExceptionHandler(), true);
    }

    private static final class DefaultForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

        private final AtomicInteger idx = new AtomicInteger();
        private final String namePrefix;

        public DefaultForkJoinWorkerThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            // Note: The ForkJoinPool will create these threads as daemon threads.
            ForkJoinWorkerThread thread = new InternalForkJoinWorkerThread(pool);
            thread.setName(namePrefix + '-' + idx.getAndIncrement());
            return thread;
        }
    }

    private static final class DefaultUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            logger.error("Uncaught exception in thread: {}", t.getName(), e);
        }
    }
}
