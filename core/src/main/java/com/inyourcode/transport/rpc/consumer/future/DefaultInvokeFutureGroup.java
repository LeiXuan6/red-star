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

import static com.inyourcode.common.util.Preconditions.checkArgument;

/**
 * jupiter
 * consumer.future
 *
 * @see InvokeFutureGroup
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("unchecked")
public class DefaultInvokeFutureGroup<V> implements InvokeFutureGroup<V> {

    private final InvokeFuture<V>[] futures;

    public static <T> DefaultInvokeFutureGroup<T> with(InvokeFuture<T>[] futures) {
        return new DefaultInvokeFutureGroup<>(futures);
    }

    private DefaultInvokeFutureGroup(InvokeFuture<V>[] futures) {
        checkArgument(futures != null && futures.length > 0, "empty futures");
        this.futures = futures;
    }

    @Override
    public Class<V> returnType() {
        return futures[0].returnType();
    }

    @Override
    public V getResult() throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    public InvokeFuture<V>[] futures() {
        return futures;
    }

    @Override
    public InvokeFuture<V> addListener(JListener<V> listener) {
        for (InvokeFuture<V> f : futures) {
            f.addListener(listener);
        }
        return this;
    }

    @Override
    public InvokeFuture<V> addListeners(JListener<V>... listeners) {
        for (InvokeFuture<V> f : futures) {
            f.addListeners(listeners);
        }
        return this;
    }

    @Override
    public InvokeFuture<V> removeListener(JListener<V> listener) {
        for (InvokeFuture<V> f : futures) {
            f.removeListener(listener);
        }
        return this;
    }

    @Override
    public InvokeFuture<V> removeListeners(JListener<V>... listeners) {
        for (InvokeFuture<V> f : futures) {
            f.removeListeners(listeners);
        }
        return this;
    }
}
