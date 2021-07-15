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
import com.inyourcode.transport.api.JAcceptor;
import com.inyourcode.transport.rpc.control.FlowController;
import com.inyourcode.transport.rpc.metadata.ServiceWrapper;
import com.inyourcode.transport.rpc.provider.ProviderInterceptor;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * The jupiter rpc server.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JServer {

    /**
     * Service registry.
     */
    interface ServiceRegistry {

        /**
         * Sets up the service provider.
         */
        ServiceRegistry provider(Object serviceProvider, ProviderInterceptor... interceptors);

        /**
         * Sets the weight of this provider at current server(0 < weight <= 100).
         */
        ServiceRegistry weight(int weight);

        /**
         * Suggest that the number of connections
         */
        ServiceRegistry connCount(int connCount);

        /**
         * Sets a private {@link Executor} to this provider.
         */
        ServiceRegistry executor(Executor executor);

        /**
         * Sets a private {@link FlowController} to this provider.
         */
        ServiceRegistry flowController(FlowController<JRequest> flowController);

        /**
         * Register this provider to local scope.
         */
        ServiceWrapper register();
    }

    interface ProviderInitializer<T> {

        /**
         * Init service provider bean.
         */
        void init(T provider);
    }

    /**
     * Returns the acceptor.
     */
    JAcceptor acceptor();

    /**
     * Sets the acceptor.
     */
    JServer withAcceptor(JAcceptor acceptor);

    /**
     * Sets global {@link ProviderInterceptor}s to this server.
     */
    void withGlobalInterceptors(ProviderInterceptor... globalInterceptors);

    /**
     * Returns the global {@link FlowController} if have one.
     */
    FlowController<JRequest> globalFlowController();

    /**
     * Sets a global {@link FlowController} to this server.
     */
    void withGlobalFlowController(FlowController<JRequest> flowController);

    /**
     * To obtains a service registry.
     */
    ServiceRegistry serviceRegistry();

    /**
     * Lookup the service.
     */
    ServiceWrapper lookupService(Directory directory);

    /**
     * Removes the registered service.
     */
    ServiceWrapper removeService(Directory directory);

    /**
     * Returns all the registered services.
     */
    List<ServiceWrapper> allRegisteredServices();


    /**
     * Start the server.
     */
    void start() throws InterruptedException;

    /**
     * Start the server.
     */
    void start(boolean sync) throws InterruptedException;

    /**
     * Unpublish all services and shutdown acceptor.
     */
    void shutdownGracefully();
}
