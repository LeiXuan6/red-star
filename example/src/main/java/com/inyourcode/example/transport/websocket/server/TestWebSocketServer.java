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
package com.inyourcode.example.transport.websocket.server;

import com.inyourcode.example.transport.websocket.C2LLoginHandler;
import com.inyourcode.transport.netty.websocket.NettyWebsocketAcceptor;
import com.inyourcode.transport.session.SessionHandlerScanner;

/**
 * @author JackLei
 */
public class TestWebSocketServer {

    public static void main(String[] args) throws InterruptedException {
        SessionHandlerScanner.registerHandler(new C2LLoginHandler());
        NettyWebsocketAcceptor nettyWebsocketAcceptor = new NettyWebsocketAcceptor(8080);
        nettyWebsocketAcceptor.start();
    }
}
