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
import com.inyourcode.common.util.JServiceLoader;
import com.inyourcode.common.util.Lists;
import com.inyourcode.common.util.NamedThreadFactory;
import com.inyourcode.common.util.Strings;
import com.inyourcode.transport.api.Directory;
import com.inyourcode.transport.api.JAcceptor;
import com.inyourcode.transport.rpc.control.FlowController;
import com.inyourcode.transport.rpc.metadata.ServiceMetadata;
import com.inyourcode.transport.rpc.metadata.ServiceWrapper;
import com.inyourcode.transport.rpc.provider.ProviderInterceptor;
import com.inyourcode.transport.rpc.provider.processor.DefaultProviderProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.inyourcode.common.util.Preconditions.checkNotNull;
import static com.inyourcode.common.util.StackTraceUtil.stackTrace;


/**
 * Jupiter默认服务端实现.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class DefaultServer implements JServer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultServer.class);

    static {
        // touch off TracingUtil.<clinit>
        // because getLocalAddress() and getPid() sometimes too slow
        ClassInitializeUtil.initClass("tracing.TracingUtil", 500);
    }

    // 服务延迟初始化的默认线程池
    private final Executor defaultInitializerExecutor =
            Executors.newSingleThreadExecutor(new NamedThreadFactory("initializer"));

    // provider本地容器
    private final ServiceProviderContainer providerContainer = new DefaultServiceProviderContainer();

    // 全局拦截器
    private ProviderInterceptor[] globalInterceptors;
    // 全局流量控制
    private FlowController<JRequest> globalFlowController;

    protected JAcceptor acceptor;

    @Override
    public JAcceptor acceptor() {
        return acceptor;
    }

    @Override
    public JServer withAcceptor(JAcceptor acceptor) {
        acceptor.withProcessor(new DefaultProviderProcessor(this));
        this.acceptor = acceptor;
        return this;
    }

    @Override
    public void withGlobalInterceptors(ProviderInterceptor... globalInterceptors) {
        this.globalInterceptors = globalInterceptors;
    }

    @Override
    public FlowController<JRequest> globalFlowController() {
        return globalFlowController;
    }

    @Override
    public void withGlobalFlowController(FlowController<JRequest> globalFlowController) {
        this.globalFlowController = globalFlowController;
    }

    @Override
    public ServiceRegistry serviceRegistry() {
        return new DefaultServiceRegistry();
    }

    @Override
    public ServiceWrapper lookupService(Directory directory) {
        return providerContainer.lookupService(directory.directory());
    }

    @Override
    public ServiceWrapper removeService(Directory directory) {
        return providerContainer.removeService(directory.directory());
    }

    @Override
    public List<ServiceWrapper> allRegisteredServices() {
        return providerContainer.getAllServices();
    }

    @Override
    public void start() throws InterruptedException {
        acceptor.start();
    }

    @Override
    public void start(boolean sync) throws InterruptedException {
        acceptor.start(sync);
    }

    @Override
    public void shutdownGracefully() {
        acceptor.shutdownGracefully();
    }

    public void setAcceptor(JAcceptor acceptor) {
        withAcceptor(acceptor);
    }

    ServiceWrapper registerService(
            String group,
            String providerName,
            String version,
            Object serviceProvider,
            ProviderInterceptor[] interceptors,
            Map<String, List<Class<?>[]>> methodsParameterTypes,
            int weight,
            int connCount,
            Executor executor,
            FlowController<JRequest> flowController) {

        ProviderInterceptor[] allInterceptors = null;
        List<ProviderInterceptor> tempList = Lists.newArrayList();
        if (globalInterceptors != null) {
            Collections.addAll(tempList, globalInterceptors);
        }
        if (interceptors != null) {
            Collections.addAll(tempList, interceptors);
        }
        if (!tempList.isEmpty()) {
            allInterceptors = tempList.toArray(new ProviderInterceptor[tempList.size()]);
        }

        ServiceWrapper wrapper =
                new ServiceWrapper(group, providerName, version, serviceProvider, allInterceptors, methodsParameterTypes);

        wrapper.setWeight(weight);
        wrapper.setConnCount(connCount);
        wrapper.setExecutor(executor);
        wrapper.setFlowController(flowController);

        providerContainer.registerService(wrapper.getMetadata().directory(), wrapper);

        return wrapper;
    }

    class DefaultServiceRegistry implements ServiceRegistry {

        private Object serviceProvider;                     // 服务对象
        private ProviderInterceptor[] interceptors;         // 拦截器
        private int weight;                                 // 权重
        private int connCount;                              // 建议客户端维持的长连接数量
        private Executor executor;                          // 该服务私有的线程池
        private FlowController<JRequest> flowController;    // 该服务私有的流量控制器

        @Override
        public ServiceRegistry provider(Object serviceProvider, ProviderInterceptor... interceptors) {
            this.serviceProvider = serviceProvider;
            this.interceptors = interceptors;
            return this;
        }

        @Override
        public ServiceRegistry weight(int weight) {
            this.weight = weight;
            return this;
        }

        @Override
        public ServiceRegistry connCount(int connCount) {
            this.connCount = connCount;
            return this;
        }

        @Override
        public ServiceRegistry executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        public ServiceRegistry flowController(FlowController<JRequest> flowController) {
            this.flowController = flowController;
            return this;
        }

        @Override
        public ServiceWrapper register() {
            checkNotNull(serviceProvider, "serviceProvider");

            Class<?> providerClass = serviceProvider.getClass();

            ServiceProviderImpl implAnnotation = null;
            ServiceProvider ifAnnotation = null;
            String providerName = null;
            Map<String, List<Class<?>[]>> methodsParameterTypes = new HashMap<>();
            for (Class<?> cls = providerClass; cls != Object.class; cls = cls.getSuperclass()) {
                if (implAnnotation == null) {
                    implAnnotation = cls.getAnnotation(ServiceProviderImpl.class);
                }

                Class<?>[] interfaces = cls.getInterfaces();
                if (interfaces != null) {
                    for (Class<?> providerInterface : interfaces) {
                        ifAnnotation = providerInterface.getAnnotation(ServiceProvider.class);
                        if (ifAnnotation == null) {
                            continue;
                        }

                        providerName = ifAnnotation.name();
                        providerName = Strings.isNotBlank(providerName) ? providerName : providerInterface.getName();

                        // method's parameterTypes
                        for (Method method : providerInterface.getMethods()) {
                            String methodName = method.getName();
                            List<Class<?>[]> list = methodsParameterTypes.get(methodName);
                            if (list == null) {
                                list = Lists.newArrayList();
                                methodsParameterTypes.put(methodName, list);
                            }
                            list.add(method.getParameterTypes());
                        }
                        break;
                    }
                }

                if (implAnnotation != null && ifAnnotation != null) {
                    break;
                }
            }

            checkNotNull(implAnnotation, providerClass.getName() + " must be annotated with @ServiceProviderImpl");
            checkNotNull(ifAnnotation, providerClass.getName() + "'s interface must be annotated with @ServiceProvider");

            String group = ifAnnotation.group();
            String version = implAnnotation.version();

            checkNotNull(group, "group");
            checkNotNull(version, "version");

            return registerService(
                    group,
                    providerName,
                    version,
                    serviceProvider,
                    interceptors,
                    methodsParameterTypes,
                    weight,
                    connCount,
                    executor,
                    flowController
            );
        }
    }

    /**
     * Local service provider container.
     *
     * 本地provider容器
     */
    interface ServiceProviderContainer {

        /**
         * 注册服务(注意并不是发布服务到注册中心, 只是注册到本地容器)
         */
        void registerService(String uniqueKey, ServiceWrapper serviceWrapper);

        /**
         * 本地容器查找服务
         */
        ServiceWrapper lookupService(String uniqueKey);

        /**
         * 从本地容器移除服务
         */
        ServiceWrapper removeService(String uniqueKey);

        /**
         * 获取本地容器中所有服务
         */
        List<ServiceWrapper> getAllServices();
    }

    // 本地provider容器默认实现
    class DefaultServiceProviderContainer implements ServiceProviderContainer {

        private final ConcurrentMap<String, ServiceWrapper> serviceProviders = new ConcurrentHashMap<>();

        @Override
        public void registerService(String uniqueKey, ServiceWrapper serviceWrapper) {
            serviceProviders.put(uniqueKey, serviceWrapper);

            logger.debug("ServiceProvider [{}, {}] is registered.", uniqueKey, serviceWrapper.getServiceProvider());
        }

        @Override
        public ServiceWrapper lookupService(String uniqueKey) {
            return serviceProviders.get(uniqueKey);
        }

        @Override
        public ServiceWrapper removeService(String uniqueKey) {
            ServiceWrapper provider = serviceProviders.remove(uniqueKey);
            if (provider == null) {
                logger.warn("ServiceProvider [{}] not found.", uniqueKey);
            } else {
                logger.debug("ServiceProvider [{}, {}] is removed.", uniqueKey, provider.getServiceProvider());
            }
            return provider;
        }

        @Override
        public List<ServiceWrapper> getAllServices() {
            return Lists.newArrayList(serviceProviders.values());
        }
    }
}
