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
package com.inyourcode.example.transport.websocket;

import com.inyourcode.transport.session.api.RequestMapping;
import com.inyourcode.transport.session.api.Session;
import org.springframework.stereotype.Controller;

/**
 * @author JackLei
 */
@Controller
public class C2LLoginHandler {

    @RequestMapping(invokeId = 1000, builder = C2LLogin.class)
    public L2CLogin login(Session session, C2LLogin msg){
        System.out.println("handle login logic.");
        System.out.println("recive message from client : "+ msg);
        return null;
    }
}
