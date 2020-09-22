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

package com.inyourcode.transport.registry.api;

import java.util.Collection;

/**
 * Registry service.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public interface RegistryService extends Registry {

    /**
     * Register service to registry server.
     */
    void register(RegisterMeta meta);

    /**
     * Unregister service to registry server.
     */
    void unregister(RegisterMeta meta);

    /**
     * Subscribe a service from registry server.
     */
    void subscribe(RegisterMeta.ServiceMeta serviceMeta, NotifyListener listener);

    /**
     * Find a service in the local scope.
     */
    Collection<RegisterMeta> lookup(RegisterMeta.ServiceMeta serviceMeta);

    /**
     * Returns {@code true} if {@link org.jupiter.registry.RegistryService} is shutdown.
     */
    boolean isShutdown();

    /**
     * Shutdown.
     */
    void shutdownGracefully();
}
