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

package com.inyourcode.transport.registry;



import com.inyourcode.common.util.Strings;
import com.inyourcode.transport.api.UnresolvedAddress;
import com.inyourcode.transport.registry.api.AbstractRegistryService;
import com.inyourcode.transport.registry.api.RegisterMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.inyourcode.common.util.Preconditions.checkArgument;
import static com.inyourcode.common.util.Preconditions.checkNotNull;


/**
 * Default registry service.
 *
 * jupiter
 * org.jupiter.registry.jupiter
 *
 * @author jiachun.fjc
 */
public class DefaultRegistryService extends AbstractRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRegistryService.class);

    private final ConcurrentMap<UnresolvedAddress, DefaultRegistry> clients = new ConcurrentHashMap<>();

    @Override
    protected void doSubscribe(RegisterMeta.ServiceMeta serviceMeta) {
        Collection<DefaultRegistry> allClients = clients.values();
        checkArgument(!allClients.isEmpty(), "init needed");

        logger.info("Subscribe: {}.", serviceMeta);

        for (DefaultRegistry c : allClients) {
            c.doSubscribe(serviceMeta);
        }
    }

    @Override
    protected void doRegister(RegisterMeta meta) {
        Collection<DefaultRegistry> allClients = clients.values();
        checkArgument(!allClients.isEmpty(), "init needed");

        logger.info("Register: {}.", meta);

        for (DefaultRegistry c : allClients) {
            c.doRegister(meta);
        }
    }

    @Override
    protected void doUnregister(RegisterMeta meta) {
        Collection<DefaultRegistry> allClients = clients.values();
        checkArgument(!allClients.isEmpty(), "init needed");

        logger.info("Unregister: {}.", meta);

        for (DefaultRegistry c : allClients) {
            c.doUnregister(meta);
        }
    }

    @Override
    public void connectToRegistryServer(String connectString) {
        checkNotNull(connectString, "connectString");

        String[] array = Strings.split(connectString, ',');
        for (String s : array) {
            String[] addressStr = Strings.split(s, ':');
            String host = addressStr[0];
            int port = Integer.parseInt(addressStr[1]);
            UnresolvedAddress address = new UnresolvedAddress(host, port);
            DefaultRegistry client = clients.get(address);
            if (client == null) {
                DefaultRegistry newClient = new DefaultRegistry(this);
                client = clients.putIfAbsent(address, newClient);
                if (client == null) {
                    client = newClient;
                    client.connect(address);
                }
            }
        }
    }

    @Override
    public void destroy() {
        for (DefaultRegistry c : clients.values()) {
            c.shutdownGracefully();
        }
    }
}
