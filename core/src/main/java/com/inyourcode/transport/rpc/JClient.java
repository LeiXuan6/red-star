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


import com.inyourcode.transport.api.Directory;
import com.inyourcode.transport.api.JConnection;
import com.inyourcode.transport.api.JConnector;
import com.inyourcode.transport.api.UnresolvedAddress;
import com.inyourcode.transport.registry.api.NotifyListener;
import com.inyourcode.transport.registry.api.OfflineListener;
import com.inyourcode.transport.registry.api.RegisterMeta;
import com.inyourcode.transport.registry.api.Registry;

import java.util.Collection;

/**
 * The jupiter rpc client.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JClient extends Registry {

    /**
     * Everyone should got a app name.
     */
    String appName();

    /**
     * Returns the connector.
     */
    JConnector<JConnection> connector();

    /**
     * Sets the connector.
     */
    JClient withConnector(JConnector<JConnection> connector);

    /**
     * Find a service in the local scope.
     */
    Collection<RegisterMeta> lookup(Directory directory);

    /**
     * Sets auto manage the connections.
     */
    JConnector.ConnectionWatcher watchConnections(Class<?> interfaceClass);

    /**
     * Sets auto manage the connections.
     */
    JConnector.ConnectionWatcher watchConnections(Class<?> interfaceClass, String version);

    /**
     * Sets auto manage the connections.
     */
    JConnector.ConnectionWatcher watchConnections(Directory directory);

    /**
     * Wait until the connections is available or timeout,
     * if available return true, otherwise return false.
     */
    boolean awaitConnections(Directory directory, long timeoutMillis);

    /**
     * Subscribe a service from registry server.
     */
    void subscribe(Directory directory, NotifyListener listener);

    /**
     * Provider offline notification.
     */
    void offlineListening(UnresolvedAddress address, OfflineListener listener);

    /**
     * Shutdown.
     */
    void shutdownGracefully();
}
