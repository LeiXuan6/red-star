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

package com.inyourcode.example.transport.rpc;

import com.inyourcode.transport.api.JConnector;
import com.inyourcode.transport.api.exception.ConnectFailedException;
import com.inyourcode.transport.netty.JNettyTcpConnector;
import com.inyourcode.transport.rpc.DefaultClient;
import com.inyourcode.transport.rpc.InvokeType;
import com.inyourcode.transport.rpc.JClient;
import com.inyourcode.transport.rpc.JListener;
import com.inyourcode.transport.rpc.consumer.ProxyFactory;
import com.inyourcode.transport.rpc.consumer.future.InvokeFuture;
import com.inyourcode.transport.rpc.consumer.future.InvokeFutureContext;

/**
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class AsyncJupiterClient {

    public static void main(String[] args) {
        JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());
        // 连接RegistryServer
        client.connectToRegistryServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionWatcher watcher = client.watchConnections(ServiceTest.class, "1.0.0.daily");
        // 等待连接可用
        if (!watcher.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        ServiceTest service = ProxyFactory.factory(ServiceTest.class)
                .version("1.0.0.daily")
                .client(client)
                .invokeType(InvokeType.ASYNC)
                .newProxyInstance();

        try {
            ServiceTest.ResultClass result = service.sayHello();
            System.out.println("sync result: " + result);
            InvokeFuture<ServiceTest.ResultClass> future = InvokeFutureContext.future(ServiceTest.ResultClass.class);
            future.addListener(new JListener<ServiceTest.ResultClass>() {

                @Override
                public void complete(ServiceTest.ResultClass result) {
                    System.out.println("callback: " + result);
                }

                @Override
                public void failure(Throwable cause) {
                    cause.printStackTrace();
                }
            });
            System.out.println("future.get: " + future.getResult());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
