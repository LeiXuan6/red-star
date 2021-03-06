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
package com.inyourcode.agent;

import com.inyourcode.dirty.api.DirtyAble;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * @author JackLei
 */
public class DirtyMethodInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger("DIRTY-LOG");

    @RuntimeType
    public Object intercept(@This Object obj,
                            @AllArguments Object[] allArguments,
                            @SuperCall Callable<?> zuper,
                            @Origin Method method) throws Throwable {
        try {
            Object ret = zuper.call();
            ((DirtyAble)obj).markDirty();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(" mark dirty, class = {}, method = {}, arguments = {}", obj.getClass(), method, allArguments);
            }
            return ret;
        } catch (Throwable t) {
            LOGGER.error("markdirty error", t);
            throw t;
        }
    }
}
