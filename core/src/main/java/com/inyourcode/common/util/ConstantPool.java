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

/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.inyourcode.common.util;

import java.util.Map;

import static com.inyourcode.common.util.Preconditions.checkArgument;
import static com.inyourcode.common.util.Preconditions.checkNotNull;

/**
 * A pool of {@link Constant}s.
 *
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 */
public abstract class ConstantPool<T extends Constant<T>> {

    private final Map<String, T> constants = Maps.newHashMap();

    private int nextId = 1;

    /**
     * Shortcut of {@link #valueOf(String) valueOf(firstNameComponent.getName() + "#" + secondNameComponent)}.
     */
    public T valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        checkNotNull(firstNameComponent, "firstNameComponent");
        checkNotNull(secondNameComponent, "secondNameComponent");

        return valueOf(firstNameComponent.getName() + '#' + secondNameComponent);
    }

    /**
     * Returns the {@link Constant} which is assigned to the specified {@code name}.
     * If there's no such {@link Constant}, a new one will be created and returned.
     * Once created, the subsequent calls with the same {@code name} will always return the previously created one
     * (i.e. singleton.)
     *
     * @param name the name of the {@link Constant}
     */
    public T valueOf(String name) {
        T c;

        synchronized (constants) {
            if (exists(name)) {
                c = constants.get(name);
            } else {
                c = newInstance0(name);
            }
        }

        return c;
    }

    /**
     * Creates a new {@link Constant} for the given {@param name} or fail with an
     * {@link IllegalArgumentException} if a {@link Constant} for the given {@param name} exists.
     */
    public T newInstance(String name) {
        if (exists(name)) {
            throw new IllegalArgumentException(String.format("'%s' is already in use", name));
        }

        return newInstance0(name);
    }

    /**
     * Returns {@code true} if exists for the given {@code name}.
     */
    public boolean exists(String name) {
        checkNotNull(name, "name");
        checkArgument(!name.isEmpty(), "empty name");

        synchronized (constants) {
            return constants.containsKey(name);
        }
    }

    private T newInstance0(String name) {
        checkNotNull(name, "name");
        synchronized (constants) {
            T c = newConstant(nextId, name);
            constants.put(name, c);
            nextId++;
            return c;
        }
    }

    protected abstract T newConstant(int id, String name);
}
