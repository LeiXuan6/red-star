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
package com.inyourcode.cache.client;

import com.inyourcode.common.util.Strings;
import com.inyourcode.transport.api.UnresolvedAddress;
import com.inyourcode.cache.provider.RemoteCacheProvider;
import com.inyourcode.transport.netty.JNettyTcpConnector;
import com.inyourcode.transport.rpc.DefaultClient;
import com.inyourcode.transport.rpc.DispatchType;
import com.inyourcode.transport.rpc.InvokeType;
import com.inyourcode.transport.rpc.JClient;
import com.inyourcode.transport.rpc.consumer.ProxyFactory;

import java.util.Map;

import static com.inyourcode.common.util.Preconditions.checkNotNull;

/**
 * @author JackLei
 */
public class DefaultCacheClient implements JCacheClient {
    private JClient client;
    private RemoteCacheProvider remoteCacheProvider;


    @Override
    public Map getHash(Object key) {
        return remoteCacheProvider.getHash(key);
    }

    @Override
    public Object getHashValue(Object key, Object hashKey) {
        return remoteCacheProvider.getHashValue(key, hashKey);
    }

    @Override
    public void putHashAll(Object key, Map hashMap) {
        remoteCacheProvider.putHashAll(key ,hashMap);
    }

    @Override
    public void delHashValue(Object key, Object hashKey) {
        remoteCacheProvider.delHashValue(key, hashKey);
    }

    @Override
    public void set(Object key, Object val) {
        remoteCacheProvider.set(key, val);
    }

    @Override
    public Object get(Object key) {
        return remoteCacheProvider.get(key);
    }

    @Override
    public void connectToCacheServer(String connectString) {
        checkNotNull(connectString, "connectString");
        String[] addressStr = Strings.split(connectString, ':');
        String host = addressStr[0];
        int port = Integer.parseInt(addressStr[1]);
        UnresolvedAddress address = new UnresolvedAddress(host, port);


        this.client = new DefaultClient().withConnector(new JNettyTcpConnector());
        client.connector().connect(address,false);

        remoteCacheProvider = ProxyFactory.factory(RemoteCacheProvider.class)
                .version("1.0.0")
                .client(client)
                .dispatchType(DispatchType.ROUND)
                .invokeType(InvokeType.SYNC)
                .addProviderAddress(address)
                .newProxyInstance();

    }

}
