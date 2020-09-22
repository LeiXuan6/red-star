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
package com.inyourcode.cluster.provider;

import com.inyourcode.cluster.ClusterNodeInfo;
import com.inyourcode.cluster.ClusterService;
import com.inyourcode.cluster.api.INodeType;
import com.inyourcode.transport.rpc.ServiceProviderImpl;

/**
 * @author JackLei
 */
@ServiceProviderImpl(version = "1.0.0")
public class ClusterSelectProviderIml implements ClusterSelectProvider {

    @Override
    public ClusterNodeInfo select(INodeType type) {
        return ClusterService.select(type);
    }
}
