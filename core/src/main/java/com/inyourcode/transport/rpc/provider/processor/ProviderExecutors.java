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

package com.inyourcode.transport.rpc.provider.processor;


import com.inyourcode.common.util.JServiceLoader;
import com.inyourcode.transport.rpc.executor.ExecutorFactory;

import java.util.concurrent.Executor;

/**
 * jupiter
 * provider.processor
 *
 * @author jiachun.fjc
 */
public class ProviderExecutors {

    private static final Executor executor;

    static {
        ExecutorFactory factory = (ExecutorFactory) JServiceLoader.loadFirst(ProviderExecutorFactory.class);
        executor = factory.newExecutor(ExecutorFactory.Target.PROVIDER);
    }

    public static Executor executor() {
        return executor;
    }

    public static void execute(Runnable command) {
        executor.execute(command);
    }
}
