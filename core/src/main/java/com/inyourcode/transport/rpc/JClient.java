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

/**
 * The jupiter rpc client.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JClient{

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
     * Shutdown.
     */
    void shutdownGracefully();
}
