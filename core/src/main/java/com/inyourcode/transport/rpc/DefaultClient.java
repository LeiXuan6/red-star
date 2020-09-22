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

import com.inyourcode.common.util.ClassInitializeUtil;
import com.inyourcode.common.util.JConstants;
import com.inyourcode.common.util.JServiceLoader;
import com.inyourcode.common.util.JUnsafe;
import com.inyourcode.common.util.Strings;
import com.inyourcode.transport.api.Directory;
import com.inyourcode.transport.api.JConnection;
import com.inyourcode.transport.api.JConnectionManager;
import com.inyourcode.transport.api.JConnector;
import com.inyourcode.transport.api.UnresolvedAddress;
import com.inyourcode.transport.api.channel.JChannelGroup;
import com.inyourcode.transport.registry.api.AbstractRegistryService;
import com.inyourcode.transport.registry.api.NotifyListener;
import com.inyourcode.transport.registry.api.OfflineListener;
import com.inyourcode.transport.registry.api.RegisterMeta;
import com.inyourcode.transport.registry.api.RegistryService;
import com.inyourcode.transport.rpc.consumer.processor.DefaultConsumerProcessor;
import com.inyourcode.transport.rpc.metadata.ServiceMetadata;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static com.inyourcode.common.util.Preconditions.checkNotNull;


/**
 * Jupiter默认客户端实现.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class DefaultClient implements JClient {

    static {
        // touch off TracingUtil.<clinit>
        // because getLocalAddress() and getPid() sometimes too slow
        ClassInitializeUtil.initClass("tracing.TracingUtil", 500);
    }

    // 服务订阅(SPI)
    private final RegistryService registryService = JServiceLoader.loadFirst(RegistryService.class);
    private final String appName;

    protected JConnector<JConnection> connector;

    public DefaultClient() {
        this(JConstants.UNKNOWN_APP_NAME);
    }

    public DefaultClient(String appName) {
        this.appName = appName;
    }

    @Override
    public String appName() {
        return appName;
    }

    @Override
    public JConnector<JConnection> connector() {
        return connector;
    }

    @Override
    public JClient withConnector(JConnector<JConnection> connector) {
        connector.withProcessor(new DefaultConsumerProcessor());
        this.connector = connector;
        return this;
    }

    @Override
    public Collection<RegisterMeta> lookup(Directory directory) {
        RegisterMeta.ServiceMeta serviceMeta = transformToServiceMeta(directory);

        return registryService.lookup(serviceMeta);
    }

    @Override
    public JConnector.ConnectionWatcher watchConnections(Class<?> interfaceClass) {
        return watchConnections(interfaceClass, JConstants.DEFAULT_VERSION);
    }

    @Override
    public JConnector.ConnectionWatcher watchConnections(Class<?> interfaceClass, String version) {
        checkNotNull(interfaceClass, "interfaceClass");
        ServiceProvider annotation = interfaceClass.getAnnotation(ServiceProvider.class);
        checkNotNull(annotation, interfaceClass + " is not a ServiceProvider interface");
        String providerName = annotation.name();
        providerName = Strings.isNotBlank(providerName) ? providerName : interfaceClass.getName();
        version = Strings.isNotBlank(version) ? version : JConstants.DEFAULT_VERSION;

        return watchConnections(new ServiceMetadata(annotation.group(), providerName, version));
    }

    @Override
    public JConnector.ConnectionWatcher watchConnections(final Directory directory) {
        JConnector.ConnectionWatcher manager = new JConnector.ConnectionWatcher() {

            private final JConnectionManager connectionManager = connector.connectionManager();

            private final ReentrantLock lock = new ReentrantLock();
            private final Condition notifyCondition = lock.newCondition();
            // Attempts to elide conditional wake-ups when the lock is uncontended.
            private final AtomicBoolean signalNeeded = new AtomicBoolean(false);

            @Override
            public void start() {
                subscribe(directory, new NotifyListener() {

                    @Override
                    public void notify(RegisterMeta registerMeta, NotifyEvent event) {
                        UnresolvedAddress address = new UnresolvedAddress(registerMeta.getHost(), registerMeta.getPort());
                        final JChannelGroup group = connector.group(address);
                        if (event == NotifyEvent.CHILD_ADDED) {
                            if (!group.isAvailable()) {
                                JConnection[] connections = connectTo(address, group, registerMeta, true);
                                for (JConnection c : connections) {
                                    c.operationComplete(new Runnable() {

                                        @Override
                                        public void run() {
                                            onSucceed(group, signalNeeded.getAndSet(false));
                                        }
                                    });
                                }
                            } else {
                                onSucceed(group, signalNeeded.getAndSet(false));
                            }
                        } else if (event == NotifyEvent.CHILD_REMOVED) {
                            connector.removeChannelGroup(directory, group);
                            if (connector.directoryGroup().getRefCount(group) <= 0) {
                                connectionManager.cancelReconnect(address); // 取消自动重连
                            }
                        }
                    }

                    private JConnection[] connectTo(final UnresolvedAddress address, final JChannelGroup group, RegisterMeta registerMeta, boolean async) {
                        int connCount = registerMeta.getConnCount();
                        connCount = connCount < 1 ? 1 : connCount;

                        JConnection[] connections = new JConnection[connCount];
                        group.setWeight(registerMeta.getWeight()); // 设置权重
                        group.setCapacity(connCount);
                        for (int i = 0; i < connCount; i++) {
                            JConnection connection = connector.connect(address, async);
                            connections[i] = connection;
                            connectionManager.manage(connection);

                            offlineListening(address, new OfflineListener() {

                                @Override
                                public void offline() {
                                    connectionManager.cancelReconnect(address); // 取消自动重连
                                    if (!group.isAvailable()) {
                                        connector.removeChannelGroup(directory, group);
                                    }
                                }
                            });
                        }

                        return connections;
                    }

                    private void onSucceed(JChannelGroup group, boolean doSignal) {
                        connector.addChannelGroup(directory, group);

                        if (doSignal) {
                            final ReentrantLock _look = lock;
                            _look.lock();
                            try {
                                notifyCondition.signalAll();
                            } finally {
                                _look.unlock();
                            }
                        }
                    }
                });
            }

            @Override
            public boolean waitForAvailable(long timeoutMillis) {
                if (connector.isDirectoryAvailable(directory)) {
                    return true;
                }

                boolean available = false;
                long start = System.nanoTime();
                final ReentrantLock _look = lock;
                _look.lock();
                try {
                    while (!connector.isDirectoryAvailable(directory)) {
                        signalNeeded.set(true);
                        notifyCondition.await(timeoutMillis, TimeUnit.MILLISECONDS);

                        available = connector.isDirectoryAvailable(directory);
                        if (available || (System.nanoTime() - start) > TimeUnit.MILLISECONDS.toNanos(timeoutMillis)) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    JUnsafe.throwException(e);
                } finally {
                    _look.unlock();
                }

                return available;
            }
        };

        manager.start();

        return manager;
    }

    @Override
    public boolean awaitConnections(Directory directory, long timeoutMillis) {
        JConnector.ConnectionWatcher manager = watchConnections(directory);
        return manager.waitForAvailable(timeoutMillis);
    }

    @Override
    public void subscribe(Directory directory, NotifyListener listener) {
        registryService.subscribe(transformToServiceMeta(directory), listener);
    }

    @Override
    public void offlineListening(UnresolvedAddress address, OfflineListener listener) {
        if (registryService instanceof AbstractRegistryService) {
            ((AbstractRegistryService) registryService).offlineListening(transformToAddress(address), listener);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void shutdownGracefully() {
        connector.shutdownGracefully();
    }

    @Override
    public void connectToRegistryServer(String connectString) {
        registryService.connectToRegistryServer(connectString);
    }

    // for spring-support
    public void setConnector(JConnector<JConnection> connector) {
        withConnector(connector);
    }

    private static RegisterMeta.ServiceMeta transformToServiceMeta(Directory directory) {
        RegisterMeta.ServiceMeta serviceMeta = new RegisterMeta.ServiceMeta();
        serviceMeta.setGroup(checkNotNull(directory.getGroup(), "group"));
        serviceMeta.setServiceProviderName(checkNotNull(directory.getServiceProviderName(), "serviceProviderName"));
        serviceMeta.setVersion(checkNotNull(directory.getVersion(), "version"));

        return serviceMeta;
    }

    private static RegisterMeta.Address transformToAddress(UnresolvedAddress address) {
        return new RegisterMeta.Address(address.getHost(), address.getPort());
    }
}
