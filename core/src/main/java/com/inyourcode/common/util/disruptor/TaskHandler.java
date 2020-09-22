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

package com.inyourcode.common.util.disruptor;

import com.inyourcode.common.util.MessageEvent;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.WorkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callback interface to be implemented for processing events as they become available in the RingBuffer.
 *
 * jupiter
 * org.jupiter.common.concurrent.disruptor
 *
 * @author jiachun.fjc
 */
public class TaskHandler implements
        EventHandler<MessageEvent<Runnable>>, WorkHandler<MessageEvent<Runnable>>, LifecycleAware {

    private static final Logger logger = LoggerFactory.getLogger(TaskHandler.class);

    @Override
    public void onEvent(MessageEvent<Runnable> event, long sequence, boolean endOfBatch) throws Exception {
        event.getMessage().run();
    }

    @Override
    public void onEvent(MessageEvent<Runnable> event) throws Exception {
        event.getMessage().run();
    }

    @Override
    public void onStart() {
        if (logger.isDebugEnabled()) {
            logger.debug("Task handler on start: {}.", Thread.currentThread().getName());
        }
    }

    @Override
    public void onShutdown() {
        if (logger.isDebugEnabled()) {
            logger.debug("Task handler on shutdown: {}.", Thread.currentThread().getName());
        }
    }
}
