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


import com.inyourcode.transport.rpc.JListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.inyourcode.common.util.StackTraceUtil.stackTrace;

/**
 * 用于实现failover集群容错方案的 {@link InvokeFuture}.
 *
 * jupiter
 * consumer.future
 *
 * @see FailSafeClusterInvoker
 *
 * @author jiachun.fjc
 */
public class FailOverInvokeFuture<V> extends AbstractInvokeFuture<V> {

    private static final Logger logger = LoggerFactory.getLogger(FailOverInvokeFuture.class);

    private final Class<V> returnType;

    public static <T> FailOverInvokeFuture<T> with(Class<T> returnType) {
        return new FailOverInvokeFuture<>(returnType);
    }

    private FailOverInvokeFuture(Class<V> returnType) {
        this.returnType = returnType;
    }

    public void setSuccess(V result) {
        set(result);
    }

    public void setFailure(Throwable cause) {
        setException(cause);
    }

    @Override
    public Class<V> returnType() {
        return returnType;
    }

    @Override
    public V getResult() throws Throwable {
        return get();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void notifyListener0(JListener<V> listener, int state, Object x) {
        try {
            if (state == NORMAL) {
                listener.complete((V) x);
            } else {
                listener.failure((Throwable) x);
            }
        } catch (Throwable t) {
            logger.error("An exception was thrown by {}.{}, {}.",
                    listener.getClass().getName(), state == NORMAL ? "complete()" : "failure()", stackTrace(t));
        }
    }
}
