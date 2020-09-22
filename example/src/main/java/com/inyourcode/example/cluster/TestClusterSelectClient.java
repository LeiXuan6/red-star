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
package com.inyourcode.example.cluster;

import com.inyourcode.cluster.ClusterNodeInfo;
import com.inyourcode.cluster.provider.ClusterSelectProvider;
import com.inyourcode.transport.api.UnresolvedAddress;
import com.inyourcode.transport.netty.JNettyTcpConnector;
import com.inyourcode.transport.rpc.DefaultClient;
import com.inyourcode.transport.rpc.DispatchType;
import com.inyourcode.transport.rpc.InvokeType;
import com.inyourcode.transport.rpc.JClient;
import com.inyourcode.transport.rpc.consumer.ProxyFactory;

/**
 * @author JackLei
 */
public class TestClusterSelectClient {

    public static void main(String[] args) {
        UnresolvedAddress unresolvedAddress = new UnresolvedAddress("127.0.0.1", 8001);
        JClient jClient = new DefaultClient().withConnector(new JNettyTcpConnector());
        jClient.connector().connect(unresolvedAddress, false);

        ClusterSelectProvider clusterSelectProvider = ProxyFactory.factory(ClusterSelectProvider.class)
                .version("1.0.0")
                .client(jClient)
                .invokeType(InvokeType.SYNC)
                .dispatchType(DispatchType.ROUND)
                .addProviderAddress(unresolvedAddress)
                .newProxyInstance();

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2 * 1000L);
                    ClusterNodeInfo select = clusterSelectProvider.select(TestClusterType.LOBBY);
                    System.out.println(select);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        },"T#TEST").start();

    }
}
