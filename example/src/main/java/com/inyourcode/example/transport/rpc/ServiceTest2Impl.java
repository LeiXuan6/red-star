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

package com.inyourcode.example.transport.rpc;


import com.inyourcode.transport.rpc.ServiceProviderImpl;

/**
 * jupiter
 * org.jupiter.example
 *
 * @author jiachun.fjc
 */
@ServiceProviderImpl(version = "1.0.0.daily")
public class ServiceTest2Impl implements ServiceTest2 {

    @Override
    public String sayHelloString() {
        return "Hello jupiter";
    }
}
