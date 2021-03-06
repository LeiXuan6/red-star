/*
 * Copyright (c) 2020 The red-star Project
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
package com.inyourcode.cluster;

import com.inyourcode.cluster.provider.ClusterSelectProviderIml;
import com.inyourcode.common.util.StackTraceUtil;
import com.inyourcode.transport.netty.JNettyTcpAcceptor;
import com.inyourcode.transport.rpc.DefaultServer;
import com.inyourcode.transport.rpc.JServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author JackLei
 */
public class ClusterQueryServer extends DefaultServer{

    private static final Logger logger = LoggerFactory.getLogger(ClusterQueryServer.class);

    private int port;
    private JServer server;

    public ClusterQueryServer(int port) {
        this.port = port;
        this.server = new DefaultServer().withAcceptor(new JNettyTcpAcceptor(port));
    }

    public void start() {
        server.serviceRegistry()
                .provider(new ClusterSelectProviderIml())
                .register();

        try {
            server.start(false);
        } catch (InterruptedException e) {
            logger.error("cluster server start error", StackTraceUtil.stackTrace(e));
        }
    }
}
