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

package com.inyourcode.transport.rpc.metadata;


import com.inyourcode.transport.rpc.tracing.TraceId;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Request data wrapper.
 *
 * 请求消息包装.
 *
 * jupiter
 * model.metadata
 *
 * @author jiachun.fjc
 */
public class MessageWrapper implements Serializable {

    private static final long serialVersionUID = 1009813828866652852L;

    private String appName;                 // 应用名称
    private ServiceMetadata metadata; // 目标服务元数据
    private String methodName;              // 目标方法名称
    private Object[] args;                  // 目标方法参数
    private TraceId traceId;                // 链路追踪ID(全局唯一)

    public MessageWrapper() {
        super();
    }

    public MessageWrapper(ServiceMetadata metadata) {
        this.metadata = metadata;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public ServiceMetadata getMetadata() {
        return metadata;
    }

    public String getGroup() {
        return metadata.getGroup();
    }

    public String getServiceProviderName() {
        return metadata.getServiceProviderName();
    }

    public String getVersion() {
        return metadata.getVersion();
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public TraceId getTraceId() {
        return traceId;
    }

    public void setTraceId(TraceId traceId) {
        this.traceId = traceId;
    }

    @Override
    public String toString() {
        return "MessageWrapper{" +
                "appName='" + appName + '\'' +
                ", metadata=" + metadata +
                ", methodName='" + methodName + '\'' +
                ", args=" + Arrays.toString(args) +
                ", traceId='" + traceId + '\'' +
                '}';
    }
}
