/*
 * Copyright (c) 2020The red-star Project
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
package com.inyourcode.transport.netty.handler.acceptor;

import com.inyourcode.common.util.AjaxResult;
import com.inyourcode.common.util.HttpMetehodCaller;
import com.inyourcode.transport.api.HttpRequestHandler;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author JackLei
 */
public class DefaultHttpRequestHandler implements HttpRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(DefaultHttpRequestHandler.class);
    private Map<String, HttpMetehodCaller> callerMap = new HashMap<>();

    public void register(HttpMetehodCaller caller) {
        callerMap.put(caller.getAction(), caller);
    }

    @Override
    public AjaxResult handle(String action, HttpMethod httpMethod, Map<String, String> param) {
        HttpMetehodCaller httpMetehodCaller = callerMap.get(action);
        if (httpMetehodCaller == null) {
            return AjaxResult.error("the action[{}] cannot found handler", action);
        }
        try {
            Object invoke = httpMetehodCaller.invoke(param);
            return AjaxResult.success(invoke);
        } catch (Exception e) {
            logger.error("the action[{}] handing exception", action, e);
            return AjaxResult.error("handing exception");
        }
    }
}
