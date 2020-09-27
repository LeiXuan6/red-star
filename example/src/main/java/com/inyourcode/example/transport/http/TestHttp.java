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
package com.inyourcode.example.transport.http;

/**
 * @author JackLei
 */
import com.inyourcode.common.util.HttpMetehodCaller;
import com.inyourcode.transport.api.HttpAction;
import com.inyourcode.transport.netty.NettyHttpAcceptor;
import com.inyourcode.transport.netty.handler.acceptor.DefaultHttpRequestHandler;

import java.lang.reflect.Method;
import java.util.Map;

public class TestHttp {

    public static void main(String[] args) throws InterruptedException {
        DefaultHttpRequestHandler defaultHttpRequest = new DefaultHttpRequestHandler();
        Method[] declaredMethods = HttActionTest.class.getDeclaredMethods();
        HttActionTest httActionObj = new HttActionTest();

        for (Method method : declaredMethods) {
            if (!method.isAnnotationPresent(HttpAction.class)) {
                continue;
            }
            HttpAction annotation = method.getAnnotation(HttpAction.class);
            HttpMetehodCaller httpMetehodCaller = new HttpMetehodCaller();
            httpMetehodCaller.setAction(annotation.action());
            httpMetehodCaller.setMethod(method);
            httpMetehodCaller.setObject(httActionObj);
            defaultHttpRequest.register(httpMetehodCaller);
        }

        NettyHttpAcceptor httpAcceptor = new NettyHttpAcceptor(8080, defaultHttpRequest);
        httpAcceptor.start();
    }


    public static class HttActionTest {

        @HttpAction(action = "login")
        public Object login(Map<String, String> param) {
            String playerId = param.get("playerId");
            System.out.println(playerId);
            return "playerid:" + playerId + ",login successed";
        }

    }
}

