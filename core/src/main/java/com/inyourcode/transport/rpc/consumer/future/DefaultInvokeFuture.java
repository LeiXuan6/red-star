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

package com.inyourcode.transport.rpc.consumer.future;

import com.inyourcode.common.util.JConstants;
import com.inyourcode.common.util.Signal;
import com.inyourcode.transport.api.Status;
import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.rpc.ConsumerHook;
import com.inyourcode.transport.rpc.DispatchType;
import com.inyourcode.transport.rpc.JListener;
import com.inyourcode.transport.rpc.JResponse;
import com.inyourcode.transport.rpc.exception.JupiterBizException;
import com.inyourcode.transport.rpc.exception.JupiterRemoteException;
import com.inyourcode.transport.rpc.exception.JupiterTimeoutException;
import com.inyourcode.transport.rpc.metadata.ResultWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static com.inyourcode.common.util.Preconditions.checkNotNull;
import static com.inyourcode.common.util.StackTraceUtil.stackTrace;


/**
 * jupiter
 * consumer.future
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("all")
public class DefaultInvokeFuture<V> extends AbstractInvokeFuture<V> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultInvokeFuture.class);

    private static final Signal S_TIMEOUT = Signal.valueOf(DefaultInvokeFuture.class, "server_time_out");
    private static final Signal C_TIMEOUT = Signal.valueOf(DefaultInvokeFuture.class, "client_time_out");

    private static final long DEFAULT_TIMEOUT_NANOSECONDS = TimeUnit.MILLISECONDS.toNanos(JConstants.DEFAULT_TIMEOUT);

    private static final ConcurrentMap<Long, DefaultInvokeFuture<?>> roundFutures = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, DefaultInvokeFuture<?>> broadcastFutures = new ConcurrentHashMap<>();

    private final long invokeId; // request.invokeId, 广播的场景可以重复
    private final JChannel channel;
    private final Class<V> returnType;
    private final long timeout;
    private final long startTime = System.nanoTime();

    private volatile boolean sent = false;

    private ConsumerHook[] hooks = ConsumerHook.EMPTY_HOOKS;

    public static <T> DefaultInvokeFuture<T> with(
            long invokeId, JChannel channel, Class<T> returnType, long timeoutMillis, DispatchType dispatchType) {

        return new DefaultInvokeFuture<T>(invokeId, channel, returnType, timeoutMillis, dispatchType);
    }

    private DefaultInvokeFuture(
            long invokeId, JChannel channel, Class<V> returnType, long timeoutMillis, DispatchType dispatchType) {

        this.invokeId = invokeId;
        this.channel = channel;
        this.returnType = returnType;
        this.timeout = timeoutMillis > 0 ? TimeUnit.MILLISECONDS.toNanos(timeoutMillis) : DEFAULT_TIMEOUT_NANOSECONDS;

        switch (dispatchType) {
            case ROUND:
                roundFutures.put(invokeId, this);
                break;
            case BROADCAST:
                broadcastFutures.put(subInvokeId(channel, invokeId), this);
                break;
            default:
                throw new IllegalArgumentException("unsupported " + dispatchType);
        }
    }

    @Override
    public Class<V> returnType() {
        return returnType;
    }

    @Override
    public V getResult() throws Throwable {
        try {
            return get(timeout, TimeUnit.NANOSECONDS);
        } catch (Signal s) {
            SocketAddress address = channel.remoteAddress();
            if (s == TIMEOUT) {
                throw new JupiterTimeoutException(address, sent ? Status.SERVER_TIMEOUT : Status.CLIENT_TIMEOUT);
            } else if (s == S_TIMEOUT) {
                throw new JupiterTimeoutException(address, Status.SERVER_TIMEOUT);
            } else if (s == C_TIMEOUT) {
                throw new JupiterTimeoutException(address, Status.CLIENT_TIMEOUT);
            } else {
                throw new JupiterRemoteException(s.name(), address);
            }
        }
    }

    @Override
    protected void notifyListener0(JListener<V> listener, int state, Object x) {
        try {
            if (state == NORMAL) {
                listener.complete((V) x);
            } else {
                Throwable cause = (Throwable) x;
                if (x instanceof Signal) {
                    SocketAddress address = channel.remoteAddress();
                    if (x == S_TIMEOUT) {
                        cause = new JupiterTimeoutException(address, Status.SERVER_TIMEOUT);
                    } else if (x == C_TIMEOUT) {
                        cause = new JupiterTimeoutException(address, Status.CLIENT_TIMEOUT);
                    } else {
                        cause = new JupiterRemoteException(((Signal) x).name(), address);
                    }
                }

                listener.failure(cause);
            }
        } catch (Throwable t) {
            logger.error("An exception was thrown by {}.{}, {}.",
                    listener.getClass().getName(), state == NORMAL ? "complete()" : "failure()", stackTrace(t));
        }
    }

    public void markSent() {
        sent = true;
    }

    public ConsumerHook[] hooks() {
        return hooks;
    }

    public DefaultInvokeFuture<V> hooks(ConsumerHook[] hooks) {
        checkNotNull(hooks, "hooks");

        this.hooks = hooks;
        return this;
    }

    private void doReceived(JResponse response) {
        byte status = response.status();

        if (status == Status.OK.value()) {
            ResultWrapper wrapper = response.result();
            set((V) wrapper.getResult());
        } else if (status == Status.SERVER_TIMEOUT.value()) {
            setException(S_TIMEOUT);
        } else if (status == Status.CLIENT_TIMEOUT.value()) {
            setException(C_TIMEOUT);
        } else if (status == Status.SERVICE_ERROR.value()) {
            setException(new JupiterBizException(response.toString(), channel.remoteAddress()));
        } else {
            setException(new JupiterRemoteException(response.toString(), channel.remoteAddress()));
        }

        // call hook's after method
        for (int i = 0; i < hooks.length; i++) {
            hooks[i].after(response, channel);
        }
    }

    public static void received(JChannel channel, JResponse response) {
        long invokeId = response.id();
        DefaultInvokeFuture<?> future = roundFutures.remove(invokeId);
        if (future == null) {
            // 广播场景下做出了一点让步, 多查询了一次roundFutures
            future = broadcastFutures.remove(subInvokeId(channel, invokeId));
        }
        if (future == null) {
            logger.warn("A timeout response [{}] finally returned on {}.", response, channel);
            return;
        }

        future.doReceived(response);
    }

    private static String subInvokeId(JChannel channel, long invokeId) {
        return channel.id() + invokeId;
    }

    // timeout scanner
    @SuppressWarnings("all")
    private static class TimeoutScanner implements Runnable {

        public void run() {
            for (;;) {
                try {
                    // round
                    for (DefaultInvokeFuture<?> future : roundFutures.values()) {
                        process(future);
                    }

                    // broadcast
                    for (DefaultInvokeFuture<?> future : broadcastFutures.values()) {
                        process(future);
                    }

                    Thread.sleep(30);
                } catch (Throwable t) {
                    logger.error("An exception has been caught while scanning the timeout futures {}.", stackTrace(t));
                }
            }
        }

        private void process(DefaultInvokeFuture<?> future) {
            if (future == null || future.isDone()) {
                return;
            }

            if (System.nanoTime() - future.startTime > future.timeout) {
                JResponse response = new JResponse(future.invokeId);
                response.status(future.sent ? Status.SERVER_TIMEOUT : Status.CLIENT_TIMEOUT);

                DefaultInvokeFuture.received(future.channel, response);
            }
        }
    }

    static {
        Thread t = new Thread(new TimeoutScanner(), "timeout.scanner");
        t.setDaemon(true);
        t.start();
    }
}
