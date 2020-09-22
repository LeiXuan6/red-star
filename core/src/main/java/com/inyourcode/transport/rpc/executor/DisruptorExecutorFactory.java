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


import com.inyourcode.common.util.SystemPropertyUtil;
import com.inyourcode.common.util.disruptor.TaskDispatcher;
import com.inyourcode.common.util.disruptor.WaitStrategyType;

import java.util.concurrent.Executor;

/**
 * Provide a disruptor implementation of executor.
 *
 * jupiter
 * executor
 *
 * @author jiachun.fjc
 */
public class DisruptorExecutorFactory extends AbstractExecutorFactory {

    @Override
    public Executor newExecutor(Target target) {
        return new TaskDispatcher(
                coreWorks(target),
                "processor",
                queueCapacity(target),
                maxWorks(target),
                waitStrategyType(target, WaitStrategyType.LITE_BLOCKING_WAIT));
    }

    private WaitStrategyType waitStrategyType(Target target, WaitStrategyType defaultType) {
        WaitStrategyType strategyType = null;
        switch (target) {
            case CONSUMER:
                strategyType = WaitStrategyType.parse(SystemPropertyUtil.get(CONSUMER_DISRUPTOR_WAIT_STRATEGY_TYPE));
                break;
            case PROVIDER:
                strategyType = WaitStrategyType.parse(SystemPropertyUtil.get(PROVIDER_DISRUPTOR_WAIT_STRATEGY_TYPE));
                break;
        }

        return strategyType == null ? defaultType : strategyType;
    }
}
