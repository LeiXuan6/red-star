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

package com.inyourcode.transport.rpc.provider;


import com.inyourcode.transport.rpc.tracing.TraceId;

/**
 * jupiter
 * provider
 *
 * @author jiachun.fjc
 */
public interface ProviderInterceptor {

    /**
     * This code is executed before the method is optionally called.
     *
     * @param traceId       id for tracing, may be null if without tracing
     * @param provider      provider be intercepted
     * @param methodName    name of the method to call
     * @param args          arguments to the method call
     */
    void beforeInvoke(TraceId traceId, Object provider, String methodName, Object[] args);

    /**
     * This code is executed after the method is optionally called.
     *
     * @param traceId       id for tracing, may be null if without tracing
     * @param provider      provider be intercepted
     * @param methodName    name of the called method
     * @param args          arguments to the called method
     * @param result        result of the executed method call
     */
    void afterInvoke(TraceId traceId, Object provider, String methodName, Object[] args, Object result);
}
