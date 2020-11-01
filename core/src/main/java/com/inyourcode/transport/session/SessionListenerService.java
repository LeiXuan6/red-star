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
package com.inyourcode.transport.session;

import com.inyourcode.transport.session.api.ISessionListener;
import com.inyourcode.transport.session.api.Session;

import java.util.ArrayList;
import java.util.List;

/**
 * @author JackLei
 */
public class SessionListenerService {
    private static List<ISessionListener> sessionListenerList = new ArrayList<>();

    public static void reigster(List<ISessionListener> sessionListeners) {
        sessionListenerList.addAll(sessionListenerList);
    }

    public static void register(ISessionListener sessionListener) {
        sessionListenerList.add(sessionListener);
    }

    public static void listenOpen(Session session){
        sessionListenerList.forEach( listener ->{
            listener.onOpen(session);
        });
    }

    public static void listenClose(Session session){
        sessionListenerList.forEach( listener ->{
            listener.onClose(session);
        });
    }
}
