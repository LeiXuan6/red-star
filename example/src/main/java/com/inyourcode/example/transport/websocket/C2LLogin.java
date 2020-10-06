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
package com.inyourcode.example.transport.websocket;

import org.msgpack.annotation.Message;

/**
 * @author JackLei
 */
@Message
public class C2LLogin {
    public static int PROTOL_LOGIN = 1000;
    public String token;
    public int platId;
    public int platType;

    @Override
    public String toString() {
        return "C2P_Login{" +
                "token='" + token + '\'' +
                ", platId=" + platId +
                ", platType=" + platType +
                '}';
    }
}
