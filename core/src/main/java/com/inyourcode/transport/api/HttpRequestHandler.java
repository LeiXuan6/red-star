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
package com.inyourcode.transport.api;

import com.inyourcode.common.util.AjaxResult;
import io.netty.handler.codec.http.HttpMethod;

import java.util.Map;

/**
 * @author JackLei
 */
public interface HttpRequestHandler {

    AjaxResult handle(String action, HttpMethod httpMethod, Map<String, String> param);

    default boolean intercept(String uri){
        if (uri.contains("/favicon.ico")) {
            return false;
        }
        return true;
    }
}
