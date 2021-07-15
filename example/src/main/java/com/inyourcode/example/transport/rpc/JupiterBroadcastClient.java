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


import com.inyourcode.transport.api.UnresolvedAddress;
import com.inyourcode.transport.netty.JNettyTcpConnector;
import com.inyourcode.transport.rpc.DefaultClient;
import com.inyourcode.transport.rpc.DispatchType;
import com.inyourcode.transport.rpc.InvokeType;
import com.inyourcode.transport.rpc.JClient;
import com.inyourcode.transport.rpc.consumer.ProxyFactory;

/**
 * 广播调用客户端
 *
 * jupiter
 * org.jupiter.example.broadcast
 *
 * @author jiachun.fjc
 */
public class JupiterBroadcastClient {

    public static void main(String[] args) {
        JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());

        UnresolvedAddress[] addresses = {
                new UnresolvedAddress("127.0.0.1", 18090),
                new UnresolvedAddress("127.0.0.1", 18091),
                new UnresolvedAddress("127.0.0.1", 18092),
                new UnresolvedAddress("127.0.0.1", 18090),
                new UnresolvedAddress("127.0.0.1", 18091),
                new UnresolvedAddress("127.0.0.1", 18092),
                new UnresolvedAddress("127.0.0.1", 18090)
        };

        for (UnresolvedAddress address : addresses) {
            client.connector().connect(address);
        }

        ServiceTest service = ProxyFactory.factory(ServiceTest.class)
                .version("1.0.0.daily")
                .client(client)
                .dispatchType(DispatchType.ROUND)
                .invokeType(InvokeType.SYNC)
                .addProviderAddress(addresses)
                .newProxyInstance();

        try {
            // callback方式
            System.out.println("callback-------------------------------");

            ServiceTest.ResultClass resultClass = service.sayHello();
            System.out.println(resultClass);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
