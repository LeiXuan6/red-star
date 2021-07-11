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

package com.inyourcode.transport.rpc.provider.processor.task;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.inyourcode.common.util.Reflects;
import com.inyourcode.common.util.disruptor.RejectedRunnable;
import com.inyourcode.common.util.SystemClock;
import com.inyourcode.common.util.SystemPropertyUtil;
import com.inyourcode.common.util.atomic.UnsafeIntegerFieldUpdater;
import com.inyourcode.common.util.atomic.UnsafeUpdater;
import com.inyourcode.serialization.api.Serializer;
import com.inyourcode.serialization.api.SerializerFactory;
import com.inyourcode.transport.api.Status;
import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.api.channel.JFutureListener;
import com.inyourcode.transport.api.payload.JRequestBytes;
import com.inyourcode.transport.api.payload.JResponseBytes;
import com.inyourcode.transport.rpc.JRequest;
import com.inyourcode.transport.rpc.control.ControlResult;
import com.inyourcode.transport.rpc.control.FlowController;
import com.inyourcode.transport.rpc.exception.JupiterBadRequestException;
import com.inyourcode.transport.rpc.exception.JupiterFlowControlException;
import com.inyourcode.transport.rpc.exception.JupiterServerBusyException;
import com.inyourcode.transport.rpc.exception.JupiterServiceNotFoundException;
import com.inyourcode.transport.rpc.metadata.MessageWrapper;
import com.inyourcode.transport.rpc.metadata.ResultWrapper;
import com.inyourcode.transport.rpc.metadata.ServiceMetadata;
import com.inyourcode.transport.rpc.metadata.ServiceWrapper;
import com.inyourcode.transport.rpc.metric.Metrics;
import com.inyourcode.transport.rpc.provider.ProviderInterceptor;
import com.inyourcode.transport.rpc.provider.processor.AbstractProviderProcessor;
import com.inyourcode.transport.rpc.tracing.TraceId;
import com.inyourcode.transport.rpc.tracing.TracingRecorder;
import com.inyourcode.transport.rpc.tracing.TracingUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.inyourcode.common.util.StackTraceUtil.stackTrace;


/**
 *
 * jupiter
 * provider.processor.task
 *
 * @author jiachun.fjc
 */
public class MessageTask implements RejectedRunnable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageTask.class);

    private static final boolean METRIC_NEEDED = SystemPropertyUtil.getBoolean("jupiter.metric.needed", true);

    private static final UnsafeIntegerFieldUpdater<TraceId> traceNodeUpdater =
            UnsafeUpdater.newIntegerFieldUpdater(TraceId.class, "node");

    private final AbstractProviderProcessor processor;
    private final JChannel channel;
    private final JRequest request;

    public MessageTask(AbstractProviderProcessor processor, JChannel channel, JRequest request) {
        this.processor = processor;
        this.channel = channel;
        this.request = request;
    }

    @Override
    public void run() {
        // stack copy
        final AbstractProviderProcessor _processor = processor;
        final JRequest _request = request;

        MessageWrapper msg;
        try {
            JRequestBytes _requestBytes = _request.requestBytes();

            byte s_code = _requestBytes.serializerCode();
            byte[] bytes = _requestBytes.bytes();
            _requestBytes.nullBytes();

            if (METRIC_NEEDED) {
                MetricsHolder.requestSizeHistogram.update(bytes.length);
            }

            Serializer serializer = SerializerFactory.getSerializer(s_code);
            // 在业务线程中反序列化, 减轻IO线程负担
            msg = serializer.readObject(bytes, MessageWrapper.class);
            _request.message(msg);
        } catch (Throwable t) {
            rejected(Status.BAD_REQUEST, new JupiterBadRequestException(t.getMessage()));
            return;
        }

        // 查找服务
        final ServiceWrapper service = _processor.lookupService(msg.getMetadata());
        if (service == null) {
            rejected(Status.SERVICE_NOT_FOUND, new JupiterServiceNotFoundException(String.valueOf(msg)));
            return;
        }

        // 全局流量控制
        ControlResult ctrl = _processor.flowControl(_request);
        if (!ctrl.isAllowed()) {
            rejected(Status.APP_FLOW_CONTROL, new JupiterFlowControlException(String.valueOf(ctrl)));
            return;
        }

        // provider私有流量控制
        FlowController<JRequest> childController = service.getFlowController();
        if (childController != null) {
            ctrl = childController.flowControl(_request);
            if (!ctrl.isAllowed()) {
                rejected(Status.PROVIDER_FLOW_CONTROL, new JupiterFlowControlException(String.valueOf(ctrl)));
                return;
            }
        }

        // processing
        Executor childExecutor = service.getExecutor();
        if (childExecutor == null) {
            process(service);
        } else {
            // provider私有线程池执行
            childExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    process(service);
                }
            });
        }
    }

    @Override
    public void rejected() {
        rejected(Status.SERVER_BUSY, new JupiterServerBusyException(String.valueOf(request)));
    }

    // 当服务拒绝方法被调用时一般分以下几种情况:
    //  1. 非法请求;
    //  2. 服务端处理能力出现瓶颈;
    //
    // 回复响应后会自动关闭当前连接, Jupiter客户端会自动重连并重新预热, 在加权负载均衡的情况下权重是一点一点升上来的
    private void rejected(Status status, Throwable cause) {
        // stack copy
        final JRequest _request = request;

        if (METRIC_NEEDED) {
            MetricsHolder.rejectionMeter.mark();
        }

        ResultWrapper result = new ResultWrapper();
        result.setError(cause);

        logger.warn("Service rejected: {}.", result.getError());

        byte s_code = _request.serializerCode();
        Serializer serializer = SerializerFactory.getSerializer(s_code);
        // 在业务线程中序列化, 减轻IO线程负担
        byte[] bytes = serializer.writeObject(result);

        final long invokeId = _request.invokeId();
        JResponseBytes response = new JResponseBytes(invokeId);
        response.status(status.value());
        response.bytes(s_code, bytes);
        channel.write(response, JChannel.CLOSE);
    }

    private void process(ServiceWrapper service) {
        // stack copy
        final JRequest _request = request;

        try {
            MessageWrapper msg = _request.message();
            String methodName = msg.getMethodName();
            final TraceId traceId = msg.getTraceId();

            // current traceId
            if (TracingUtil.isTracingNeeded()) {
                if (traceId != null) {
                    traceNodeUpdater.set(traceId, traceId.getNode() + 1);
                }
                TracingUtil.setCurrent(traceId);
            }

            Object provider = service.getServiceProvider();
            Object[] args = msg.getArgs();

            // 拦截器
            ProviderInterceptor[] interceptors = service.getInterceptors();

            if (interceptors != null) {
                handleBeforeInvoke(interceptors, traceId, provider, methodName, args);
            }

            String callInfo = null;
            Timer.Context timerCtx = null;
            if (METRIC_NEEDED) {
                callInfo = getCallInfo(msg.getMetadata(), methodName);
                timerCtx = Metrics.timer(callInfo).time();
            }

            Object invokeResult = null;
            try {
                List<Class<?>[]> parameterTypesList = service.getMethodParameterTypes(methodName);
                if (parameterTypesList == null) {
                    throw new NoSuchMethodException(methodName);
                }

                // 根据JLS方法调用的静态分派规则查找最匹配的方法parameterTypes
                Class<?>[] parameterTypes = Reflects.findMatchingParameterTypes(parameterTypesList, args);
                invokeResult = Reflects.fastInvoke(provider, methodName, parameterTypes, args);
            } catch (Throwable t) {
                // biz exception
                processor.handleException(channel, _request, Status.SERVICE_ERROR, t);
                return;
            } finally {
                long elapsed = -1;
                if (METRIC_NEEDED) {
                    elapsed = timerCtx.stop();
                }

                if (interceptors != null) {
                    handleAfterInvoke(interceptors, traceId, provider, methodName, args, invokeResult);
                }

                // tracing recoding
                if (traceId != null && TracingUtil.isTracingNeeded()) {
                    if (callInfo == null) {
                        callInfo = getCallInfo(msg.getMetadata(), methodName);
                    }
                    TracingRecorder recorder = TracingUtil.getRecorder();
                    recorder.recording(TracingRecorder.Role.PROVIDER, traceId.asText(), callInfo, elapsed, channel);
                }
            }

            ResultWrapper result = new ResultWrapper();
            result.setResult(invokeResult);
            byte s_code = _request.serializerCode();
            Serializer serializer = SerializerFactory.getSerializer(s_code);
            byte[] bytes = serializer.writeObject(result);

            if (METRIC_NEEDED) {
                MetricsHolder.responseSizeHistogram.update(bytes.length);
            }

            JResponseBytes response = new JResponseBytes(_request.invokeId());
            response.status(Status.OK.value());
            response.bytes(s_code, bytes);
            channel.write(response, new JFutureListener<JChannel>() {

                @Override
                public void operationSuccess(JChannel channel) throws Exception {
                    if (METRIC_NEEDED) {
                        MetricsHolder.processingTimer.update(
                                SystemClock.millisClock().now() - _request.timestamp(), TimeUnit.MILLISECONDS);
                    }
                }

                @Override
                public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                    logger.error(
                            "Service response[traceId: {}] sent failed, elapsed: {} millis, channel: {}, cause: {}.",
                            traceId, SystemClock.millisClock().now() - _request.timestamp(), channel, cause
                    );
                }
            });
        } catch (Throwable t) {
            processor.handleException(channel, _request, Status.SERVER_ERROR, t);
        }
    }

    @SuppressWarnings("all")
    private static void handleBeforeInvoke(
            ProviderInterceptor[] interceptors, TraceId traceId, Object provider, String methodName, Object[] args) {

        for (int i = 0; i < interceptors.length; i++) {
            interceptors[i].beforeInvoke(traceId, provider, methodName, args);
            try {
                interceptors[i].beforeInvoke(traceId, provider, methodName, args);
            } catch (Throwable t) {
                logger.warn("Interceptor[{}#beforeInvoke]: {}.", Reflects.simpleClassName(interceptors[i]), stackTrace(t));
            }
        }
    }

    @SuppressWarnings("all")
    private static void handleAfterInvoke(
            ProviderInterceptor[] interceptors, TraceId traceId, Object provider, String methodName, Object[] args, Object invokeResult) {

        for (int i = interceptors.length - 1; i >= 0; i--) {
            interceptors[i].beforeInvoke(traceId, provider, methodName, args);
            try {
                interceptors[i].afterInvoke(traceId, provider, methodName, args, invokeResult);
            } catch (Throwable t) {
                logger.warn("Interceptor[{}#afterInvoke]: {}.", Reflects.simpleClassName(interceptors[i]), stackTrace(t));
            }
        }
    }

    private static String getCallInfo(ServiceMetadata metadata, String methodName) {
        String directory = metadata.directory();
        return new StringBuilder()
                .append(directory)
                .append('#')
                .append(methodName).toString();
    }

    // - Metrics -------------------------------------------------------------------------------------------------------
    static class MetricsHolder {
        // 请求处理耗时统计(从request被解码开始, 到response数据被刷到OS内核缓冲区为止)
        static final Timer processingTimer              = Metrics.timer("processing");
        // 请求被拒绝次数统计
        static final Meter rejectionMeter               = Metrics.meter("rejection");
        // 请求数据大小统计(不包括Jupiter协议头的16个字节)
        static final Histogram requestSizeHistogram     = Metrics.histogram("request.size");
        // 响应数据大小统计(不包括Jupiter协议头的16个字节)
        static final Histogram responseSizeHistogram    = Metrics.histogram("response.size");
    }
}
