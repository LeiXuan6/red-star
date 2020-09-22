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

package com.inyourcode.example.transport.round;


import com.inyourcode.example.transport.ServiceTest;
import com.inyourcode.example.transport.ServiceTest2;
import com.inyourcode.transport.api.JConnector;
import com.inyourcode.transport.api.exception.ConnectFailedException;
import com.inyourcode.transport.netty.JNettyTcpConnector;
import com.inyourcode.transport.rpc.DefaultClient;
import com.inyourcode.transport.rpc.JClient;
import com.inyourcode.transport.rpc.consumer.ProxyFactory;

/**
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class SyncJupiterClient {

    public static void main(String[] args) {
        final JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());
        // 连接RegistryServer
        client.connectToRegistryServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionWatcher watcher1 = client.watchConnections(ServiceTest.class, "1.0.0.daily");
        JConnector.ConnectionWatcher watcher2 = client.watchConnections(ServiceTest2.class, "1.0.0.daily");
        // 等待连接可用
        if (!watcher1.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }
        if (!watcher2.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                client.shutdownGracefully();
            }
        });


        ServiceTest2 service2 = ProxyFactory.factory(ServiceTest2.class)
                .version("1.0.0.daily")
                .client(client)
                .newProxyInstance();

        try {
            String result2 = service2.sayHelloString();
            System.out.println(result2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
