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

package com.inyourcode.transport.rpc.tracing;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 默认记录tracing信息的方式是打印日志, 可基于SPI扩展.
 *
 * jupiter
 * tracing
 *
 * @author jiachun.fjc
 */
public class DefaultInternalTracingRecorder extends TracingRecorder {

    private static final Logger logger = LoggerFactory.getLogger(DefaultInternalTracingRecorder.class);

    @Override
    public void recording(Role role, Object... args) {
        if (logger.isInfoEnabled()) {
            if (role == Role.CONSUMER) {
                String traceInfo = new StringBuilder()
                        .append("[Consumer] - ")
                        .append(args[0])
                        .append(", callInfo: ")
                        .append(args[1])
                        .append('#')
                        .append(args[2])
                        .append(", on ")
                        .append(args[3]).toString();

                logger.info(traceInfo);
            } else if (role == Role.PROVIDER) {
                String traceInfo = new StringBuilder()
                        .append("[Provider] - ")
                        .append(args[0])
                        .append(", callInfo: ")
                        .append(args[1])
                        .append(", elapsed: ")
                        .append(TimeUnit.NANOSECONDS.toMillis((long) args[2]))
                        .append(" millis, on ")
                        .append(args[3]).toString();

                logger.info(traceInfo);
            }
        }
    }
}
