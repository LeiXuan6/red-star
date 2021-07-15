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

package com.inyourcode.transport.rpc;

import com.inyourcode.common.util.ClassInitializeUtil;
import com.inyourcode.common.util.JConstants;
import com.inyourcode.common.util.JUnsafe;
import com.inyourcode.common.util.Strings;
import com.inyourcode.transport.api.Directory;
import com.inyourcode.transport.api.JConnection;
import com.inyourcode.transport.api.JConnectionManager;
import com.inyourcode.transport.api.JConnector;
import com.inyourcode.transport.api.UnresolvedAddress;
import com.inyourcode.transport.api.channel.JChannelGroup;
import com.inyourcode.transport.rpc.consumer.processor.DefaultConsumerProcessor;
import com.inyourcode.transport.rpc.metadata.ServiceMetadata;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static com.inyourcode.common.util.Preconditions.checkNotNull;


/**
 * Jupiter默认客户端实现.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class DefaultClient implements JClient {

    static {
        // touch off TracingUtil.<clinit>
        // because getLocalAddress() and getPid() sometimes too slow
        ClassInitializeUtil.initClass("tracing.TracingUtil", 500);
    }

    // 服务订阅(SPI)
    private final String appName;

    protected JConnector<JConnection> connector;

    public DefaultClient() {
        this(JConstants.UNKNOWN_APP_NAME);
    }

    public DefaultClient(String appName) {
        this.appName = appName;
    }

    @Override
    public String appName() {
        return appName;
    }

    @Override
    public JConnector<JConnection> connector() {
        return connector;
    }

    @Override
    public JClient withConnector(JConnector<JConnection> connector) {
        connector.withProcessor(new DefaultConsumerProcessor());
        this.connector = connector;
        return this;
    }

    @Override
    public void shutdownGracefully() {
        connector.shutdownGracefully();
    }

    // for spring-support
    public void setConnector(JConnector<JConnection> connector) {
        withConnector(connector);
    }

}
