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

package com.inyourcode.transport.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Jupiter的连接管理器, 用于自动管理(地址归组)连接.
 *
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public class JConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(JConnectionManager.class);

    private final ConcurrentMap<UnresolvedAddress, CopyOnWriteArrayList<JConnection>> connections = new ConcurrentHashMap<>();

    /**
     * 设置为由jupiter自动管理连接
     */
    public void manage(JConnection connection) {
        UnresolvedAddress address = connection.getAddress();
        CopyOnWriteArrayList<JConnection> list = connections.get(address);
        if (list == null) {
            CopyOnWriteArrayList<JConnection> newList = new CopyOnWriteArrayList<>();
            list = connections.putIfAbsent(address, newList);
            if (list == null) {
                list = newList;
            }
        }
        list.add(connection);
    }

    /**
     * 取消自动重连
     */
    public void cancelReconnect(UnresolvedAddress address) {
        CopyOnWriteArrayList<JConnection> list = connections.remove(address);
        if (list != null) {
            for (JConnection c : list) {
                c.setReconnect(false);
            }
            logger.warn("Cancel reconnect to: {}.", address);
        }
    }

    /**
     * 取消自动重连
     */
    public void cancelAllReconnect() {
        for (UnresolvedAddress address : connections.keySet()) {
            cancelReconnect(address);
        }
    }
}
