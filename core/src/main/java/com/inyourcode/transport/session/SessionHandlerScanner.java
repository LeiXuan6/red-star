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
package com.inyourcode.transport.session;

import com.inyourcode.transport.session.api.AsyncRequest;
import com.inyourcode.transport.session.api.FixedRequest;
import com.inyourcode.transport.session.api.NoAuthRequest;
import com.inyourcode.transport.session.api.RequestMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * 请求处理的映射关系
 * @author JackLei
 */
public class SessionHandlerScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionHandlerScanner.class);
    private static final Map<Long, ProcesserWrapper> REGISTRY_MAP = new HashMap<>();
    private static Set<Long> INVOKE_ID_SET = new HashSet<>();

    public static void init(ApplicationContext applicationContext) {
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(Controller.class);
        for (Object processerObj : beansWithAnnotation.values()) {
            registerHandler(processerObj);
        }
    }

    public static void registerHandler(Object handlerObj) {
        Class clazz = handlerObj.getClass();
        boolean annotationPresent = clazz.isAnnotationPresent(Controller.class);
        if (!annotationPresent) {
            return;
        }
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method method : declaredMethods) {
            boolean haveHandlerAnno = method.isAnnotationPresent(RequestMapping.class);
            if (!haveHandlerAnno) {
                continue;
            }

            RequestMapping handlerAnno = method.getAnnotation(RequestMapping.class);
            long invokeId = handlerAnno.invokeId();
            if (INVOKE_ID_SET.contains(invokeId)) {
                throw new RuntimeException(String.format("Invoke id is duplicate,method = %s,id = %d ", method, invokeId));
            }

            AsyncRequest asycRequest = method.getAnnotation(AsyncRequest.class);
            FixedRequest fixedRequest = method.getAnnotation(FixedRequest.class);
            NoAuthRequest noAuthRequest = method.getAnnotation(NoAuthRequest.class);

            INVOKE_ID_SET.add(invokeId);
            Class messageClazz = handlerAnno.builder();
            ProcesserWrapper wapper = new ProcesserWrapper(invokeId, messageClazz, method, handlerObj, asycRequest != null, fixedRequest != null, noAuthRequest == null);
            REGISTRY_MAP.put(invokeId, wapper);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Message handler  reigster successful ,handler = {}", wapper);
            }
        }
    }

    public static ProcesserWrapper getProcesserWapper(long invokeId) {
        return REGISTRY_MAP.get(invokeId);
    }

    /**
     * 方法包装器
     */
    public static class ProcesserWrapper {
        private long invokerId;
        private Class messageClazz;
        private Method method;
        private Object processer;
        private boolean asyncReq;
        private boolean fixedReq;
        private boolean noAuthReq;

        public ProcesserWrapper(long invokerId, Class messageClazz, Method method, Object processer, boolean asyncReq, boolean fixed, boolean noAuthReq) {
            this.invokerId = invokerId;
            this.messageClazz = messageClazz;
            this.method = method;
            this.processer = processer;
            this.asyncReq = asyncReq;
            this.fixedReq = fixed;
            this.noAuthReq = noAuthReq;
        }

        public long getInvokerId() {
            return invokerId;
        }

        public Class getMessageClazz() {
            return messageClazz;
        }

        public Object getProcesser() {
            return processer;
        }

        public Method getMethod() {
            return method;
        }

        public boolean isAsyncReq() {
            return asyncReq;
        }

        public boolean isFixedReq() {
            return fixedReq;
        }

        public boolean isNoAuthReq() {
            return noAuthReq;
        }

        public Object invoke(Object... param) throws InvocationTargetException, IllegalAccessException {
            return this.method.invoke(this.processer, param);
        }


        @Override
        public String toString() {
            return "ProcesserWrapper{" +
                    "invokerId=" + invokerId +
                    ", clazz=" + messageClazz.getSimpleName() +
                    ", method=" + method.getName() +
                    '}';
        }
    }
}

